/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package action;

import actionForm.CrearRevistaActionForm;
import connection.ConnectionPSQL;
import email.Mail;
import email.MailVarios;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import pdf.MergePDF;
import utilidades.BorrarDirectorio;
import utilidades.Constantes;
import utilidades.OS;

/**
 *
 * @author Rafa
 */
public class CrearRevistaAction extends org.apache.struts.action.Action {

    /* forward name="success" path="" */
    private static final String SUCCESS = "success";
    private final static String FAILURE = "failure";
    private final static String CANCEL = "cancel";

    /**
     * This is the action called from the Struts framework.
     *
     * @param mapping The ActionMapping used to select this instance.
     * @param form The optional ActionForm bean for this request.
     * @param request The HTTP Request we are processing.
     * @param response The HTTP Response we are processing.
     * @throws java.lang.Exception
     * @return
     */
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        if (isCancelled(request)) {
            return mapping.findForward(CANCEL);
        } else {
            CrearRevistaActionForm formBean = (CrearRevistaActionForm) form;
            String rutaWEBINF = formBean.getRutaNumeros();
            
            System.out.println("RUTA= " + rutaWEBINF);

            //Se crean el pdf de todos los articulos correspondientes
            String numero = consultaListaNumeros(rutaWEBINF);
            //Si no hay nigun numero nuevo parará aquí
            if (numero.equals("False")) {
                formBean.setErrorMsg(Constantes.getERROR_CREAR_NUMERO());
                return mapping.findForward(FAILURE);
            }

            //Se crea el fichero de los usuarios participantes
            String separator = OS.getDirectorySeparator();
            String rutaNumeros = rutaWEBINF + separator + "numeros";
            crearFicheroParticipantes(rutaWEBINF, rutaNumeros);

            //Se crea el indice
            crearIndice(rutaWEBINF, rutaNumeros);

            //Se crea el numero con todos los pdfs creados anteriormente
            String rutaRaiz = formBean.getRutaRaiz();
            String rutaNumerosAll = rutaWEBINF + separator + "numeros" + separator + "all";
            String numeroPDF = MergePDF.crearNumeroRevista(rutaRaiz, rutaNumerosAll, numero);

            //Se comprueba que el numero se haya creado correctamente
            if (numeroPDF.isEmpty()) {
                formBean.setErrorMsg(Constantes.getERROR_CREAR_NUMERO());
                return mapping.findForward(FAILURE);
            }

            File numeroCreado = new File(numeroPDF);
            if (numeroCreado.exists() == false) {
                formBean.setErrorMsg(Constantes.getERROR_CREAR_NUMERO());
                return mapping.findForward(FAILURE);
            }

            String nameServer = request.getContextPath();
            String urlNumeros = "http://moleqla.upo.es:8080/revista/work/work.do";//nameServer + "/revista/work/work.do";
            String asunto = Constantes.getEMAIL_ASUNTO_NUMERO_PUBLICADO();
            String texto = Constantes.getEMAIL_TEXTO_NUMERO_PUBLICADO(urlNumeros);

            //Obtenemos todos los usuarios
            List<String> listaAllEmails = consultaAllEmails();
            if (listaAllEmails.isEmpty() == false) {
                MailVarios.enviarMail(listaAllEmails, texto, asunto);
            }
            //Se eliminan los articulos que han servido para este numero
            comprobacion_eliminacion(numeroPDF, rutaNumerosAll);

            formBean.setMsg(Constantes.getCREACION_NUMERO_OK());
            return mapping.findForward(SUCCESS);
        }
    }

    /**
     * Retorna "False" si no hay un numero nuevo
     *
     * @param rutaWEBINF
     * @return
     * @throws SQLException
     */
    private String consultaListaNumeros(String rutaWEBINF) throws SQLException {
        String separator = OS.getDirectorySeparator();
        String res = "";

        String fichero = rutaWEBINF + separator + "numeros" + separator + "pdf.py";
        String rutaDestino = rutaWEBINF + separator + "numeros" + separator + "all";
        String rutaEntrevista = rutaWEBINF + separator + "numeros";
        String[] cmd = new String[3];
        cmd[0] = fichero;
        cmd[1] = rutaDestino;
        cmd[2] = rutaEntrevista;

        Process f;
        try {
            f = Runtime.getRuntime().exec(cmd);
            try {
                f.waitFor();
            } catch (InterruptedException ex) {
                Logger.getLogger(CrearRevistaAction.class.getName()).log(Level.SEVERE, null, ex);
            }

            // retrieve output from python script
            BufferedReader bfr = new BufferedReader(new InputStreamReader(f.getInputStream()));
            String line = "", numero = "";
            while ((line = bfr.readLine()) != null) {
                numero = line;
                System.out.println("Articulos creados. Numero " + line);
            }
            res = numero;
        } catch (IOException ex) {
            Logger.getLogger(CrearRevistaAction.class.getName()).log(Level.SEVERE, null, ex);
        }

        return res;
    }

    private boolean comprobacion_eliminacion(String rutaDestino, String rutaAllArticulos) {
        File resNum = new File(rutaDestino);
        boolean borrado = true;
        if (resNum.exists()) {
            File dir_all_articulos = new File(rutaAllArticulos);
            BorrarDirectorio.borrarDirectorio(dir_all_articulos);
        }

        return borrado;
    }

    private String crearFicheroParticipantes(String rutaWEBINF, String rutaNumeros) {
        String separator = OS.getDirectorySeparator();
        String res = "";
        String fichero = Constantes.getRUTA_EJECUTABLE_PHP5() + " " + rutaWEBINF + separator + "numeros" + separator + "participantes.php" + " " + rutaNumeros;

        Process f;
        try {
            f = Runtime.getRuntime().exec(fichero);
            try {
                f.waitFor();
            } catch (InterruptedException ex) {
                Logger.getLogger(CrearRevistaAction.class.getName()).log(Level.SEVERE, null, ex);
            }

            // retrieve output from python script
            BufferedReader bfr = new BufferedReader(new InputStreamReader(f.getInputStream()));
            String line = "", numero = "";
            while ((line = bfr.readLine()) != null) {
                numero = line;
                System.out.println("Fichero participantes creado " + line);
            }
            res = numero;
        } catch (IOException ex) {
            Logger.getLogger(CrearRevistaAction.class.getName()).log(Level.SEVERE, null, ex);
        }

        return res;
    }

    private String crearIndice(String rutaWEBINF, String rutaNumeros) {
        String separator = OS.getDirectorySeparator();
        String res = "";
        String fichero = Constantes.getRUTA_EJECUTABLE_PHP5() + " " + rutaWEBINF + separator + "numeros" + separator + "indice.php" + " " + rutaNumeros;

        Process f;
        try {
            f = Runtime.getRuntime().exec(fichero);
            try {
                f.waitFor();
            } catch (InterruptedException ex) {
                Logger.getLogger(CrearRevistaAction.class.getName()).log(Level.SEVERE, null, ex);
            }

            // retrieve output from python script
            BufferedReader bfr = new BufferedReader(new InputStreamReader(f.getInputStream()));
            String line = "", numero = "";
            while ((line = bfr.readLine()) != null) {
                numero = line;
                System.out.println("Fichero participantes creado " + line);
            }
            res = numero;
        } catch (IOException ex) {
            Logger.getLogger(CrearRevistaAction.class.getName()).log(Level.SEVERE, null, ex);
        }

        return res;
    }

    private List<String> consultaAllEmails() {
        List<String> listaAllEmails = new ArrayList();
        try (Connection connection = ConnectionPSQL.connection()) {
            ResultSet rs = connection.createStatement().executeQuery(
                    "SELECT login FROM res_users");

            if (rs != null) {

                while (rs.next()) {
                    if (Mail.compruebaEmail(rs.getString(1))) {
                        listaAllEmails.add(rs.getString(1));
                    }

                }
                rs.close();
            }
        } catch (SQLException ex) {
            Logger.getLogger(AboutAction.class.getName()).log(Level.SEVERE, null, ex);
        }

        return listaAllEmails;
    }
}
