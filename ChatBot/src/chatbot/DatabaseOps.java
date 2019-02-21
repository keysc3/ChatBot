/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatbot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

/**
 * Purpose: Implements methods to connect to a local database and insert/select
 * specific data.
 * 
 * @author Colin Keys
 * 
 * Variables            Description
 * 
 */
public class DatabaseOps {
    
     /**
     * getConnection - Opens a connection to a local database
     * @return Connection - A connection to the database
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InstantiationException
     * @throws java.lang.IllegalAccessException
     * @throws java.sql.SQLException
     */
    public static Connection getConnection() throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException{
        Connection conn = null;
        Class.forName("com.mysql.jdbc.Driver").newInstance();
        conn = DriverManager.getConnection(ChatBot.config.getProperty("connString"), ChatBot.config.getProperty("username"), ChatBot.config.getProperty("password"));
        return conn;
    }
    
    /**
     * checkUserExists - Checks to see if a user is already an entry in the user database
     * @param author - user entity
     * @param conn- connection to the wanted database
     * @return boolean - a boolean, whether or not the user is in the database already
     * @throws SQLException
     */
    public Boolean checkUserExists(User author, Connection conn) throws SQLException{
        //Select the user that sent the message to see if they exist
        PreparedStatement prepstate = conn.prepareStatement("select * from user where UserID = ?");
        prepstate.setString(1, author.getId());
        ResultSet rs = prepstate.executeQuery();
        
        //If select found something this will return true.
        return rs.next();
    }
    
    /**
     * checkServerExists - Checks to see if a server is already an entry in the server database
     * @param guild - Server entity
     * @param conn- connection to the wanted database
     * @return boolean - a boolean, whether or not the user is in the database already
     * @throws SQLException
     */
    public Boolean checkServerExists(Guild guild, Connection conn) throws SQLException{
        //Select the user that sent the message to see if they exist
        PreparedStatement prepstate = conn.prepareStatement("select * from server where ServerID = ?");
        prepstate.setString(1, guild.getId());
        ResultSet rs = prepstate.executeQuery();
        
        //If select found something this will return true.
        return rs.next();
    }
    
    /**
     * insertUser - Insert the new user into the user database
     * @param user - User entity
     * @param conn- connection to the wanted database
     * @return prepState - A PreparedStatment to be executed
     * @throws SQLException
     */
    public PreparedStatement insertUser(User user, Connection conn) throws SQLException{
        //Insert the users information into the user database
        PreparedStatement prepState;
        prepState = conn.prepareStatement("insert into user (UserID, Name, Discriminator) values (?, ?, ?)");
        prepState.setString(1, user.getId());
        prepState.setString(2, user.getName());
        prepState.setString(3, user.getDiscriminator());
        
        return prepState;
    }
    
    /**
     * insertServer - Insert the new server into the server database
     * @param guild - Server entity
     * @param conn- connection to the wanted database
     * @return prepState - A PreparedStatment to be executed
     * @throws SQLException
     */
    public PreparedStatement insertServer(Guild guild, Connection conn) throws SQLException{
        //Insert the users information into the user database
        PreparedStatement prepState;
        prepState = conn.prepareStatement("insert into server (ServerID, Name) values (?, ?)");
        System.out.println(guild.getId() + " " + guild.getName());
        prepState.setString(1, guild.getId());
        prepState.setString(2, guild.getName());
        
        return prepState;
    }
    
    /**
     * insertMessage - Insert the new message into the messages database
     * @param event - MessageReceivedEvent to get information from
     * @param conn- connection to the wanted database
     * @return prepState - A PreparedStatment to be executed
     * @throws SQLException
     */
    public PreparedStatement insertMessage(MessageReceivedEvent event, Connection conn) throws SQLException{
        //Insert specific information
        PreparedStatement prepState;
        Message message = event.getMessage();
        Date date = Date.from(message.getCreationTime().toInstant());
        java.sql.Date sqlDate = new java.sql.Date(date.getTime());
        
        //Insert the neccesary message information into the messages database
        prepState = conn.prepareStatement("insert into messages (UserID, ServerID, Channel, CreationTime) values (?, ?, ?, ?)");
        prepState.setString(1, event.getAuthor().getId());
        prepState.setString(2, event.getGuild().getId());
        prepState.setString(3, message.getChannel().getName());
        prepState.setDate(4, sqlDate);
        
        return prepState;
    }
    
    /**
     * topMessageSenders - Get the Top 5 users who have sent the most messages across
     * all tracked channels in the specific server
     * @param conn- connection to the wanted database
     * @param serverId - discord server id of the server the message was sent in
     * @return ResultSet - A ResultSet of the query results to be iterated over
     * @throws SQLException
     */
    public ResultSet topMessageSenders(String serverId, Connection conn) throws SQLException{
        //Select the top 5 users
        PreparedStatement prepState = conn.prepareStatement("select U.UserId, U.Name, count(M.UserID) as TotalMsg "
                + "from user as U, messages as M "
                + "where U.UserID = M.UserID " + "and ServerID = " + serverId + " "
                + "group by U.UserID "
                + "order by TotalMsg desc "
                + "limit 5");
        
        return prepState.executeQuery();
    }
    
    /**
     * topChannels - Get the Top 3 channels that received the most messages in the specific server
     * @param conn- connection to the wanted database
     * @param serverId - discord server id of the server the message was sent in
     * @return ResultSet - A ResultSet of the query results to be iterated over
     * @throws SQLException
     */
    public ResultSet topChannels(String serverId, Connection conn) throws SQLException{
        //Select the top 3 channels
        PreparedStatement prepState = conn.prepareStatement("select Channel, count(UserID) as TotalMsg "
                + "from messages "
                + "where ServerID = " + serverId + " "
                + "group by Channel "
                + "order by TotalMsg desc "
                + "limit 3");
        
        return prepState.executeQuery();
    }
    
    /**
     * userPerChannel - Get the number of messages the given user sent per channel
     * @param userId - discord id of the user's statistics that are being collected
     * @param serverId - discord server id of the server the message was sent in
     * @param conn- connection to the wanted database
     * @return ResultSet - A ResultSet of the query results to be iterated over
     * @throws SQLException
     */
    public ResultSet userPerChannel(String userId, String serverId, Connection conn) throws SQLException{
        //Select the users amount of messages per channel
        PreparedStatement prepState = conn.prepareStatement("select Channel, count(UserID) as TotalMsg "
                + "from messages "
                + "where UserID = " + userId + " and ServerID = " + serverId + " "
                + "group by Channel "
                + "order by TotalMsg desc");
        
        return prepState.executeQuery();
    }
    
    /**
     * userTotal - Get the total number of messages the given user sent
     * @param userId - discord id of the user's statistics that are being collected
     * @param serverId - discord server id of the server the message was sent in
     * @param conn- connection to the wanted database
     * @return ResultSet - A ResultSet of the query results to be iterated over
     * @throws SQLException
     */
    public ResultSet userTotal(String userId, String serverId, Connection conn) throws SQLException{
        //Count all message the user sent
        PreparedStatement prepState = conn.prepareStatement("select count(UserID) as TotalMsg "
                + "from messages "
                + "where UserID = " + userId
                +" and ServerID = " + serverId);
        
        return prepState.executeQuery();
    }
    
    /**
     * recordMessage - Record the message being sent into the database
     * @param event - MessageReceivedEvent instance generated when the bot
     * a message the bot can read it received.
     * @param conn- connection to the wanted database
     * @throws java.sql.SQLException
     */
    public void recordMessage(MessageReceivedEvent event, Connection conn) throws SQLException{
        PreparedStatement prepState;
        
        //If user isnt in the database add them
        if(!checkUserExists(event.getAuthor(), conn)){
            //Add new entry
            prepState = insertUser(event.getAuthor(), conn);
            prepState.executeUpdate();

        }
        
        //If server isnt in the database add it
        if(!checkServerExists(event.getGuild(), conn)){
            //Add new entry
            prepState = insertServer(event.getGuild(), conn);
            prepState.executeUpdate();

        }
        
        //Add to message table
        prepState = insertMessage(event, conn);
        prepState.executeUpdate();
    }
    
    
}
