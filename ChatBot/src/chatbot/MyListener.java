/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chatbot;

import java.awt.Color;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang3.StringUtils;

/**
 * Purpose: This class implements a ListenerAdapter for a discord bot. It checks all messages
 * for specific commands and outputs a command specific 
 * message to the channel it received it in.
 * 
 * @author Colin Keys
 * 
 * Variables            Description
 * 
 * private
 * 
 * dbOps                DataboseOps object for database operations
 * 
 */
public class MyListener extends ListenerAdapter {
    private final DatabaseOps dbOps = new DatabaseOps();
    @Override
    public void onMessageReceived(MessageReceivedEvent event){
            //Even Specific information
            String msg = event.getMessage().getContentDisplay();
            Message sentMessage = event.getMessage();
            Guild guild = event.getGuild();
            String channelName = event.getChannel().getName();
            
            //Dont respond to other bots or this bot
            if(event.getAuthor().isBot()) 
                return;
            
        try {
            //Open database connection
            Connection conn = dbOps.getConnection();
            
            //Don't track test messages
            if(!channelName.equals("test"))
                dbOps.recordMessage(event, conn);
            
            //Only check messages starting with the bots prefix
            if(!msg.startsWith(ChatBot.config.getProperty("prefix")))
                    return;
            
            //Command specific information
            ArrayList<String> args = new ArrayList<>(Arrays.asList(msg.split(" ")));
            String command = args.get(0).substring(1);
            args.remove(0);
            int numArgs = args.size();
            
            //Switch to check for commands
            switch(command){
                //Output overall/user message stats for the channel
                case "stats":
                    //Only want one argument max
                    if(numArgs > 1){
                        event.getChannel().sendMessage("Please provide only one user/argument").queue();
                        break;
                    }
                    //Output overall message stats
                    if(numArgs == 0){
                        //Get top users and channels, format output
                        ResultSet userResults = dbOps.topMessageSenders(conn);
                        ResultSet channelResults = dbOps.topChannels(conn);
                        EmbedBuilder outputResults = outputMessageStats(guild, userResults, channelResults);
                        event.getChannel().sendMessage(outputResults.build()).queue();
                        break;
                    }
                    //Output user message stats
                    if(numArgs == 1){
                        //Need a user to be mentiond
                        if(sentMessage.getMentionedUsers().isEmpty()){
                            event.getChannel().sendMessage("Please mention a user instead of just typing it!").queue();
                            break;
                        }
                        //Get user mentioned, make sure it isnt a bot
                        List<User> mentionedUsers = sentMessage.getMentionedUsers();
                        User user = mentionedUsers.get(0);
                        if(user.isBot()){
                            event.getChannel().sendMessage("Sorry, my messages aren't being tracked!").queue();
                            break;
                        }
                        //Get users total messages sent and amount sent to channels
                        ResultSet userTotal = dbOps.userTotal(user.getId(), conn);
                        ResultSet userChannel = dbOps.userPerChannel(user.getId(), conn);
                        EmbedBuilder outputResults = outputUserStats(user, userTotal, userChannel);
                        event.getChannel().sendMessage(outputResults.build()).queue();
                        break;
                    }
                    break;
                //Delete messages from channel, up to 10 at a time
                case "delete":
                    //Make sure a number is given, <= 10
                    if(numArgs < 1 || !StringUtils.isNumericSpace(args.get(0)) || Integer.valueOf(args.get(0)) > 10){
                        event.getChannel().sendMessage("Error deleting messages! Please provide an amount less than 10").queue();
                        break;
                    }
                    //Make sure member who sent message is part of 'God' role
                    Member member = event.getMember();
                    List<String> roles = getRolesAsString(member.getRoles());
                    if(!roles.contains("Gods")){
                        event.getChannel().sendMessage("Sorry, you do not have permissions to delete messages!").queue();
                        break;
                    }
                    //Get last 10 messages then delete them
                    List<Message> msgList = getMessages(event.getChannel(), Integer.valueOf(args.get(0))+1);
                    for (Message message : msgList)
                        message.delete().queue();
                    break;
            }
            
            //Close database connection
            conn.close();
          
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException ex) {
            Logger.getLogger(MyListener.class.getName()).log(Level.SEVERE, null, ex);
        }
            
    }
    
    /**
     * Purpose: Build the embed message for server message statistics
     * @param guild - server getting the statistics from
     *  userResults - top 5 users and the number of messages they sent
     * channelResults - top 3 channels and the number of messages they received
     * @return eb - embed message to output to the channel
     * @throws java.sql.SQLException
     */
    private EmbedBuilder outputMessageStats(Guild guild, ResultSet userResults, ResultSet channelResults) throws SQLException{
        //Variables for formatting output
        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder topUsers = new StringBuilder();
        StringBuilder topChannels = new StringBuilder();
        int i = 1;
        
        //Server the statistics are for and color of embed message
        eb.setAuthor(guild.getName() + " Chat Stats", null, guild.getIconUrl());
        eb.setColor(Color.red);
        
        //Iterate over user ResultSet and format output
        while(userResults.next()){
            topUsers.append(String.valueOf(i)).append(". ")
                    .append(userResults.getString("Name")).append(": ")
                    .append(userResults.getString("TotalMsg")).append("\n");
            i++;
        }
        
        //reset for proper ordering
        i = 1;
        
        //Iterate over channel ResultSet and format output
        while(channelResults.next()){
            topChannels.append(String.valueOf(i)).append(". ")
                    .append(channelResults.getString("Channel")).append(": ")
                    .append(channelResults.getString("TotalMsg")).append("\n");
            i++;
        }
        
        //Format embed output
        eb.addField("Talkative Users", topUsers.toString(), true);
        eb.addField("Top Channels", topChannels.toString(), true);
        eb.setFooter("Made by ExplodingMuffins", "https://cdn.discordapp.com/avatars/123205408528662529/929fc31d35ce73ec8f337c47df3d955e.png");
        
        return eb;
    }
    
    /**
     * Purpose: Build the embed message for user message statistics
     * @param user - user getting the statistics from
     * @param userTotal - total number of messages the user sent
     * @param userChannel - number of messages sent in each channel
     * @return eb - embed message to output to the channel
     * @throws java.sql.SQLException
     */
    private EmbedBuilder outputUserStats(User user, ResultSet userTotal, ResultSet userChannel) throws SQLException{
        //Variables for formatting output
        EmbedBuilder eb = new EmbedBuilder();
        StringBuilder userTotalMsgs = new StringBuilder();
        StringBuilder userUsedChannels = new StringBuilder();
        int i = 1;
        
        //User the statistics are for and color of embed message
        eb.setAuthor(user.getName() + " Chat Stats", null, user.getAvatarUrl());
        eb.setColor(Color.red);
        
        //Iterate over user total messages ResultSet and format output
        while(userTotal.next()){
            userTotalMsgs.append(userTotal.getString("TotalMsg"));
        }
        
        //Iterate over channel ResultSet and format output
        while(userChannel.next()){
            userUsedChannels.append(String.valueOf(i)).append(". ")
                    .append(userChannel.getString("Channel")).append(": ")
                    .append(userChannel.getString("TotalMsg")).append("\n");
            i++;
        }
        
        //Format output if isn't in database
        if(userUsedChannels.length() == 0)
            userUsedChannels.append("N/A");
        
        //Format embed output
        eb.addField("Total Messages Sent", userTotalMsgs.toString(), false);
        eb.addField("Messages Per Channel", userUsedChannels.toString(), true);
        eb.setFooter("Made by ExplodingMuffins", "https://cdn.discordapp.com/avatars/123205408528662529/929fc31d35ce73ec8f337c47df3d955e.png");
                
        return eb;
    }
    
    /**
     * Purpose: Get the past 'num' amount of messages from the given 'channel'
     * @param channel - channel to delete messages from
     * @param num - number of messages to delete
     * @return messages - list of all messages to be deleted
     */
    public List<Message> getMessages(MessageChannel channel, int num){
    //Hold messages
    List<Message> messages = new ArrayList<>(num);
    
    //Get messages until num <= 0
    for (Message message : channel.getIterableHistory().cache(false)){
        messages.add(message);
        if (--num <= 0) break;
    }
    
    return messages;
    }
    
    /**
     * Purpose: Format the given roles list into a human readable list
     * @param roles - list of user roles
     * @return stringRoles - list of all user roles
     */
    public List<String> getRolesAsString(List<Role> roles){
    //Hold roles
    List<String> stringRoles = new ArrayList<>(roles.size());
    
    //Get role names
    for (Role role : roles)
        stringRoles.add(role.getName());
    
    return stringRoles;
    }
    
    
}
