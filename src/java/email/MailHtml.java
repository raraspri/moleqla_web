package email;

import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import utilidades.Constantes;

public class MailHtml {

    private InternetAddress[] addressTo;

    /*public static void enviarMailHtmlFile(String rutaImg, File archivoAdjunto, String usuarioDestinatario, String textoEmail, String asunto) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.setProperty("mail.smtp.starttls.enable", "true");
            props.setProperty("mail.smtp.port", "587");
            props.setProperty("mail.smtp.user", Constantes.getEMAIL_NOTIFICA());
            props.setProperty("mail.smtp.password", Constantes.getEMAIL_NOTIFICA_PASS());
            props.setProperty("mail.smtp.auth", "true");

            Session session = Session.getDefaultInstance(props, null);
            // session.setDebug(true);

            // Se compone la parte del texto
            BodyPart texto = new MimeBodyPart();
            texto.setText(textoEmail);

            // Se compone el adjunto con la imagen
            BodyPart adjunto = new MimeBodyPart();
            adjunto.setDataHandler(new DataHandler(new FileDataSource(archivoAdjunto)));
            adjunto.setFileName(archivoAdjunto.getName());

            // Se compone el adjunto con la imagen
            BodyPart html = new MimeBodyPart();
            html.setContent(texto, "text/html");

            // Header email
            String separator = OS.getDirectorySeparator();
            DataSource fds = new FileDataSource(rutaImg + separator + "logoMoleqla.jpg");
            BodyPart cabecera = new MimeBodyPart();
            cabecera.setDataHandler(new DataHandler(fds));
            cabecera.setHeader("Content-ID", "<cabecera>");

            // Una MultiParte para agrupar texto e imagen.
            MimeMultipart multiParte = new MimeMultipart();
            multiParte.addBodyPart(cabecera);
            multiParte.addBodyPart(html);
            multiParte.addBodyPart(adjunto);

            // Se compone el correo, dando to, from, subject y el
            // contenido.
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(props.getProperty("mail.smtp.user")));
            message.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(usuarioDestinatario));
            message.setSubject(asunto);
            message.setContent(multiParte);

            // Se envia el correo.
            Transport t = session.getTransport("smtp");
            t.connect(props.getProperty("mail.smtp.user"), props.getProperty("mail.smtp.password"));
            t.sendMessage(message, message.getAllRecipients());
            t.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    public static boolean enviarMailHtml(String usuarioDestinatario, String textoEmail, String asunto) {
        boolean ok = false;
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props,
          new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(Constantes.getEMAIL_NOTIFICA(), Constantes.getEMAIL_NOTIFICA_PASS());
            }
          });

        try {   
            
            // Header email             
            String logo = Constantes.getEMAIL_URL_LOGO();
            String cabecera = "<img src=\""+logo+ ""+"\" width=\"250\" height=\"100\"/> <br />";
            
            textoEmail = cabecera + textoEmail;
            
            // Se compone el adjunto con la imagen
            BodyPart html = new MimeBodyPart();
            html.setContent(textoEmail, "text/html");
            

            // Una MultiParte para agrupar texto e imagen.
            MimeMultipart multiParte = new MimeMultipart();
            multiParte.addBodyPart(html);
            
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(Constantes.getEMAIL_NOTIFICA()));
            message.setRecipients(Message.RecipientType.TO,
                InternetAddress.parse(usuarioDestinatario));
            
            message.setSubject(asunto);
            message.setContent(multiParte);

            Transport.send(message);

            System.out.println("Done");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("\n" + e.getMessage()
                    + "\n-----------------\n"
                    + Constantes.getEMAIL_ERROR()
                    + "\n-----------------\n"
                    + "Email: " + usuarioDestinatario + "\n");
        }
        return ok;
    }
}
