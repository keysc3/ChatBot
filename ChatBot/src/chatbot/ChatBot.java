/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatbot;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

/**
 *
 * @author Colin
 */
public class ChatBot {

   public static final Properties config = new Properties();
    /**
     * Purpose: Start and configure the discord bot
     * @param args the command line arguments
     * @throws net.dv8tion.jda.core.exceptions.RateLimitedException
     */
    public static void main(String[] args) throws RateLimitedException {
        //Try and catch for exceptions
        try{
            InputStream input = new FileInputStream("src/props/chatbot-config.properties");
            config.load(input);
            //Start the bot, set it to my bots token, attach wanted listeners.
            JDA api = new JDABuilder(AccountType.BOT)
                    .setToken(config.getProperty("botToken"))
                    .addEventListener(new MyListener())
                    .buildBlocking();
            System.out.println("I'm Online!\nI'm Online!");
        }
        catch (LoginException | InterruptedException e){
        } catch (FileNotFoundException ex) {
            Logger.getLogger(ChatBot.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ChatBot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
}
