package gamingplatform.controller;

import gamingplatform.dao.implementation.UserDaoImpl;
import gamingplatform.dao.interfaces.UserDao;
import gamingplatform.model.User;
import org.apache.commons.io.FileUtils;

import javax.annotation.Resource;
import javax.servlet.http.Part;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

class FileManager {

    @Resource(name = "jdbc/gamingplatform")
    private static DataSource ds;

    /**
     * dato un filePart (file proveniente da una form), controlla se il file è un .jpg
     * se si lo copia nella directory specificata e torna il nome del file salvato
     * @param filePart oggetto Part contenente dati del file
     * @param directory directory in cui verrà salvto il file
     * @return il nome del file salvato, oppure null su errore
     */
    static String fileUpload(Part filePart, String directory){

        String fileName = Paths.get(filePart.getSubmittedFileName()).getFileName().toString();

        //se non è un .jpg oppure un .png
        if(!fileName.substring(fileName.length()-4).equals(".jpg") ||
           !fileName.substring(fileName.length()-4).equals(".png")){

            return null;
        }

        try {
            //aggiungo il tempo in millisecondi prima del nome (così mantengo il .jpg) per evitare conflitti
            fileName = System.currentTimeMillis()+fileName;
            InputStream fileContent = filePart.getInputStream();
            //salvo il file nella directory specificata
            File targetFile = new File("/template/"+directory+fileName);
            FileUtils.copyInputStreamToFile(fileContent, targetFile);
        } catch (IOException e) {
            Logger.getAnonymousLogger().log(Level.WARNING,"[FileManager] fileUpload IOException "+e.getMessage());
            return null;
        }

        return fileName;
    }


}
