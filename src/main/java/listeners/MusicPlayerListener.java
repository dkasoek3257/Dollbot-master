package listeners;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.awt.*;

public class MusicPlayerListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        String message = event.getMessage().getContentRaw();
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        VoiceChannel voiceChannel = event.getMember().getVoiceState().getChannel().asVoiceChannel();
        audioManager.openAudioConnection(voiceChannel);

        DefaultAudioPlayerManager playerManager = new DefaultAudioPlayerManager();
        playerManager.enableGcMonitoring();
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());

        AudioPlayer player = playerManager.createPlayer();
        AudioSendHandler sendHandler = new AudioPlayerSendHandler(player);
        audioManager.setSendingHandler(sendHandler);

        if (message.equals("!exit")) {
            if (voiceChannel != null) {
                audioManager.closeAudioConnection();
                event.getChannel().sendMessage("Exiting voice channel").queue();
                player.stopTrack();
            } else {
                event.getChannel().sendMessage("Not currently in a voice channel").queue();
            }
        }

        if (message.startsWith("!play")) {
            String sing = message.substring("!play ".length());
            if (!sing.startsWith("https://")) {
                sing = "https://www.youtube.com/results?search_query=" + sing;
            }
            event.getChannel().sendMessage("play!").queue();
            System.out.println("play");


            if (voiceChannel == null) {
                channel.sendMessage("You need to be in a voice channel to use this command!").queue();
                return;
            }


            String trackUrl = sing;
            playerManager.loadItem(trackUrl, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    System.out.println("Track loaded: " + track.getInfo().title);
                    player.playTrack(track);

                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle(track.getInfo().title);
                    builder.setColor(Color.GREEN);

                    // send the embed message to the channel
                    channel.sendMessageEmbeds(builder.build()).queue();
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    System.out.println("Playlist loaded: " + playlist.getName());
                    for (AudioTrack track : playlist.getTracks()) {
                        player.playTrack(track);
                    }
                }

                @Override
                public void noMatches() {
                    System.out.println("No matches found");
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    System.out.println("Load failed: " + exception.getMessage());
                    exception.printStackTrace();
                }
            });

        }

    }
}
