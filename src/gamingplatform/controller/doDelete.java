package gamingplatform.controller;

import gamingplatform.dao.exception.DaoException;
import gamingplatform.dao.implementation.*;
import gamingplatform.dao.interfaces.GameDao;
import gamingplatform.dao.interfaces.LevelDao;
import gamingplatform.dao.interfaces.ReviewDao;
import gamingplatform.dao.interfaces.UserDao;
import gamingplatform.dao.interfaces.ServiceDao;
import gamingplatform.dao.interfaces.GroupsDao;
import gamingplatform.model.User;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static gamingplatform.controller.utils.SecurityLayer.checkAuth;
import static gamingplatform.controller.utils.SessionManager.getUser;
import static gamingplatform.controller.utils.SessionManager.verifySession;
import static gamingplatform.controller.utils.Utils.getLastBitFromUrl;
import static gamingplatform.controller.utils.Utils.getNlastBitFromUrl;
import static java.util.Objects.isNull;


/**
 * classe atta al processamento di richieste ajax post per l'eliminazione di elementi nel db
 * risponde a url del tipo /doDelete/tabella/idElemento
 */

public class doDelete extends HttpServlet {

    @Resource(name = "jdbc/gamingplatform")
    private static DataSource ds;

    /**
     * gestisce richieste POST alla servlet
     * @param request richiesta servlet
     * @param response risposta servlet
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        //imposto tipo di ritorno della risposta
        response.setContentType("text/html;charset=UTF-8");

        //se non è una chiamata ajax
        if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            Logger.getAnonymousLogger().log(Level.WARNING, "[doDelete] non è una chiamata ajax");
            //torno KO alla chiamata servlet
            response.getWriter().write("KO");
            return;
        }


        //carico la tabella in cui si vuole aggiungere la tupla (la url è della forma /doDelete/tabella/idElemento
        String item = getNlastBitFromUrl(request.getRequestURI(), 1);

        String id = "";
        int itemId = 0;

        //per gestire il caso review abbiamo bisogno di 2 id, l'id utente e l'id del gioco per identificare la singola recensione
        //l'uri è del tipo /doDelete/review/idGioco&idUser
        String idGame = "";
        String idUser = "";
        Boolean authReview=false;

        try {
            //gestione a parte del caso particolare di review
            if (item.equals("review")) {

                String idArray[] = getLastBitFromUrl(request.getRequestURI()).split("&");
                idGame = idArray[0];
                idUser = idArray[1];

                if (Integer.parseInt(idUser) == ((User) verifySession(request).getAttribute("user")).getId() || checkAuth(request,"review")){
                    authReview=true;
                }

            } else {
                //caso base di qualsiasi altra tabella con id semplice
                id = getLastBitFromUrl(request.getRequestURI());

                if (isNull(id) || id.equals("")) {
                    Logger.getAnonymousLogger().log(Level.WARNING, "[doDelete: " + item + "] parametri POST non validi");
                    response.getWriter().write("KO");
                    return;
                }

                //gestire caso review a parte (è della forma /doDelete/review/idGame-idUser
                itemId = Integer.parseInt(id);

            }

            //se ho autorizzazione per la review (vedi if sopra) non controllo le autorizzazioni
            //ho auth review se ho il servizio review nei permessi oppure se sto cancellando la mia review
            if(!authReview) {
                //controllo quì se l'utente è loggato e ha acesso a quella determinata tabella
                //se l'utente sta cercando di eliminare il suo profilo, glielo permetto
                if (!(item.equals("user") && getUser(request).getId() == itemId)) {
                    if (!checkAuth(request, item)) {
                        //se  il servizio a cui si sta provando ad accedere
                        //non è un servizio a cui l'utente ha accesso
                        response.getWriter().write("KO");
                        return;
                    }
                }
            }

            //gestisco i vari casi
            switch (item) {
                //caso eliminazione user
                case "user":
                    UserDao userDao = new UserDaoImpl(ds);
                    userDao.init();
                    //provo ad eliminare l'utente
                    userDao.deleteUserByKey(itemId);
                    userDao.destroy();
                    break;

                //caso eliminazione user
                case "game":
                    GameDao gameDao = new GameDaoImpl(ds);
                    gameDao.init();
                    gameDao.deleteGameById(itemId);
                    gameDao.destroy();
                    break;
                //caso eliminazione level
                case "level":
                    LevelDao levelDao = new LevelDaoImpl(ds);
                    levelDao.init();
                    levelDao.deleteLevel(itemId);
                    levelDao.destroy();
                    break;
                //caso eliminazione review
                case "review":
                    int idG = Integer.parseInt(idGame);
                    int idU = Integer.parseInt(idUser);
                    ReviewDao reviewDao = new ReviewDaoImpl(ds);
                    reviewDao.init();
                    reviewDao.deleteReview(idG, idU);
                    reviewDao.destroy();
                    break;
                //caso eliminazione service
                case "service":
                    ServiceDao serviceDao = new ServiceDaoImpl(ds);
                    serviceDao.init();
                    serviceDao.deleteServiceById(itemId);
                    serviceDao.destroy();
                    break;
                //caso eliminazione group
                case "groups":
                    GroupsDao groupsDao = new GroupsDaoImpl(ds);
                    groupsDao.init();
                    groupsDao.deleteGroupById(itemId);
                    groupsDao.destroy();
                    break;
                //default
                default:

                    Logger.getAnonymousLogger().log(Level.WARNING, "[doDelete: " + item + "] caso default nello switch");
                    //torno KO alla chiamata servlet
                    response.getWriter().write("KO");
                    return;
            }


        } catch (DaoException e) {
            Logger.getAnonymousLogger().log(Level.WARNING, "[doDelete: " + item + "] " + e.getMessage());
            //torno KO alla chiamata servlet
            response.getWriter().write("KO");
            return;
        }

        //torno OK alla chiamata servlet se arrivo alla fine senza problemi
        response.getWriter().write("OK");

    }
}