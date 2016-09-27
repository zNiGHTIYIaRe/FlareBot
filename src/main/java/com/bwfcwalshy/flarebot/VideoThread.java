package com.bwfcwalshy.flarebot;

import com.bwfcwalshy.flarebot.music.MusicManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;

public class VideoThread extends Thread {

    // Keep this instance across all threads. Efficiency bitch!
    private static MusicManager manager;

    private String searchTerm;
    private IUser user;
    private IChannel channel;
    private boolean isUrl = false;
    private boolean isShortened = false;
    public VideoThread(String term, IUser user, IChannel channel) {
        this.searchTerm = term;
        this.user = user;
        this.channel = channel;
        if(manager == null) manager = FlareBot.getInstance().getMusicManager();
        start();
    }

    public VideoThread(String termOrUrl, IUser user, IChannel channel, boolean url, boolean shortened){
        this.searchTerm = termOrUrl;
        this.user = user;
        this.channel = channel;
        this.isUrl = url;
        this.isShortened = shortened;
        if(manager == null) manager = FlareBot.getInstance().getMusicManager();
        start();
    }

    // Making sure these stay across all threads.
    private static final String SEARCH_URL = "https://www.youtube.com/results?search_query=";
    private static final String YOUTUBE_URL = "https://www.youtube.com";
    private static final String EXTENSION = ".mp3";

    @Override
    public void run(){
        long a = System.currentTimeMillis();
        //TODO: Severely clean this up!!!
        try {
            IMessage message;
            String videoName;
            String videoFile;
            String link;
            String videoId;
            if(isUrl){
                if(isShortened){
                    searchTerm = YOUTUBE_URL + searchTerm.replaceFirst("http(s)?:\\/\\/youtu.be", "");
                }
                message = MessageUtils.sendMessage(channel, "Getting video from URL.");
                Document doc = Jsoup.connect(searchTerm).get();
                videoId = searchTerm.replaceFirst("http(s)?:\\/\\/wwww.youtube.com/watch?v=", "");
                // Playlist
                if(videoId.contains("&list")) videoId.substring(0, videoId.indexOf("&list")+5);
                videoName = MessageUtils.escapeFile(doc.title().replace(" - YouTube", ""));
                videoFile = videoName + "-" + videoId;
                link = searchTerm;
            }else {
                message = MessageUtils.sendMessage(channel, "Searching YouTube for '" + searchTerm + "'");
                Document doc = Jsoup.connect(SEARCH_URL + URLEncoder.encode(searchTerm, "UTF-8")).get();

                Element videoElement = doc.getElementsByClass("yt-lockup-title").get(0);
                for(Element e : videoElement.children()){
                    if(e.className().contains("ad-badge")) {
                        videoElement = doc.getElementsByClass("yt-lockup-title").get(1);
                        break;
                    }
                }
                link = videoElement.select("a").first().attr("href");
                // I check the index of 2 chars so I need to add 2
                FlareBot.LOGGER.debug(YOUTUBE_URL + link);
                Document doc2 = Jsoup.connect(YOUTUBE_URL + link).get();
                videoName = MessageUtils.escapeFile(doc2.title().replace(" - YouTube", ""));
                videoFile = videoName + "-" + link.substring(link.indexOf("v=") + 2);
                videoId = link.substring(link.indexOf("v=")+2);

                link = YOUTUBE_URL + link;
            }
            if(new File("cached" + File.separator + videoFile + EXTENSION).exists()){
                message.edit("Loading cached video!");
                manager.addSong(channel.getGuild().getID(), videoFile + EXTENSION);
                message.edit(user.mention() + " added: **" + videoName + "** to the playlist!");
            }else{
                message.edit("Downloading video!");

                ProcessBuilder builder = new ProcessBuilder("youtube-dl", "-o", "cached/%(title)s" + "-" + videoId + ".%(ext)s", "--extract-audio", "--audio-format"
                        , "mp3", "--ffmpeg-location", "ffmpeg/bin", link);
                FlareBot.LOGGER.debug("Downloading");
                builder.redirectErrorStream(true);
                Process process = builder.start();
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))){
                    while(process.isAlive()){
                        String line;
                        if((line = reader.readLine()) != null){
                            FlareBot.LOGGER.debug(line);
                        }
                    }
                }
                manager.addSong(channel.getGuild().getID(), videoFile + EXTENSION);
                message.edit(user.mention() + " added: **" + videoName + "** to the playlist!");
            }
        } catch (IOException | RateLimitException e) {
            FlareBot.LOGGER.error(e.getMessage(), e);
        } catch (DiscordException e) {
            if(!e.getMessage().contains("CloudFlare"))
                FlareBot.LOGGER.error(e.getMessage(), e);
        } catch (MissingPermissionsException ignored) {
        }
        long b = System.currentTimeMillis();
        FlareBot.LOGGER.debug("Process took " + (b - a) + " milliseconds");
    }
}
