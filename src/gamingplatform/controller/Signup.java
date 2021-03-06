package gamingplatform.controller;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import gamingplatform.dao.exception.DaoException;
import gamingplatform.dao.implementation.UserDaoImpl;
import gamingplatform.dao.implementation.UserLevelDaoImpl;
import gamingplatform.dao.interfaces.UserDao;
import gamingplatform.dao.interfaces.UserLevelDao;
import gamingplatform.model.User;
import gamingplatform.model.UserLevel;

import static gamingplatform.controller.utils.Utils.fileUpload;
import static gamingplatform.controller.utils.SecurityLayer.*;
import static gamingplatform.controller.utils.SessionManager.popMessage;
import static gamingplatform.controller.utils.SessionManager.redirectIfLogged;
import static gamingplatform.view.FreemarkerHelper.process;
import static java.util.Objects.isNull;



/**
 * classe servlet che gestisce la registrazione di un utente
 */
//impostazioni caricamento files
@MultipartConfig(
        fileSizeThreshold=1024*1024,    // 1 MB
        maxFileSize=1024*1024*5,        // 5 MB
        maxRequestSize=1024*1024*5*5    // 25 MB
)
public class Signup extends HttpServlet {

    @Resource(name = "jdbc/gamingplatform")
    private static DataSource ds;

    //container dati che sarà processato da freemarker
    private Map<String, Object> data = new HashMap<>();

    /**
     * gestisce richieste GET ala servlet, nello specifico mostra la form di registrazione
     * @param request richiesta servlet
     * @param response risposta servlet
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        data.put("message", popMessage(request));

        redirectIfLogged(request,response);

        //process template
        process("signup.ftl", data, response, getServletContext());

    }

    /**
     * gestisce richieste POST alla servlet, nello specifico effettua la
     * registrazione dell'utente in base ai dati inseriti nella form
     * @param request richiesta servlet
     * @param response risposta servlet
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{

        request.setCharacterEncoding("UTF-8");

        //prelevo parametri POST
        String username = request.getParameter("username");
        String name = request.getParameter("name");
        String surname = request.getParameter("surname");
        String email = request.getParameter("email");
        String password = sha1Encrypt( request.getParameter("password"));
        Part avatar = request.getPart("avatar"); // recupera <input type="file" name="avatar">

        //se i parametri in input non sono validi
        if(isNull(username) || isNull(name) || isNull(surname) || isNull(email) || isNull(password) ||
           username.equals("") || name.equals("") || surname.equals("") || email.equals("") || password.equals("")){

            Logger.getAnonymousLogger().log(Level.WARNING,"[Signup] Parametri POST non validi ");
            abort("signup.ftl",data,"KO-signup",response,getServletContext());
            return;
        }
        //provo ad effettuare l'upload del file
        String avatarName = fileUpload(avatar,"avatars", getServletContext());
        if(isNull(avatarName)){

            Logger.getAnonymousLogger().log(Level.WARNING,"[Signup] Upload file fallito");
            abort("signup.ftl",data,"KO-signup",response,getServletContext());
            return;
        }

        UserDao userDao = new UserDaoImpl(ds);
        try{
            userDao.init();
            //provo ad inserire l'utente

            User user=userDao.getUser();
            UserLevelDao userLevelDao1 = new UserLevelDaoImpl(ds);
            userLevelDao1.init();

            user.setUsername(username);
            user.setName(name);
            user.setSurname(surname);
            user.setEmail(email);
            user.setPassword(password);
            user.setExp(0);
            user.setAvatar(avatarName);

            userDao.insertUser(user);


            //metto l'user al livello 0
            UserLevel userLevel1 = userLevelDao1.getUserLevel();
            userLevel1.setDate(new Timestamp(System.currentTimeMillis()));
            userLevel1.setLevelId(0);
            userLevel1.setUserId(userDao.getUserByUsernamePassword(username,password).getId());
            userLevelDao1.insertUserlevel(userLevel1);

            userDao.destroy();
            userLevelDao1.destroy();

            userDao.destroy();
        }catch(DaoException e){
            //in caso di errori nell'inserimento dell'utente
            Logger.getAnonymousLogger().log(Level.WARNING,"[Signup] Inserimento utente fallito "+e.getMessage());
            abort("signup.ftl",data,"KO-signup",response,getServletContext());
            return;
        }

        //se arrivo quì ho inserito l'user con successo
        redirect("/login","OK-signup",response,request);

    }

}