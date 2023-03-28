package listeners;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.awt.*;


public class MusicPlayerListener extends ListenerAdapter {
    private final AudioPlayerManager playerManager;
    private final AudioPlayer player;
    private final AudioSendHandler sendHandler;

    public MusicPlayerListener() {
        this.playerManager = new DefaultAudioPlayerManager();
        this.player = playerManager.createPlayer();
        this.sendHandler = new AudioPlayerSendHandler(player);
        playerManager.registerSourceManager(new YoutubeAudioSourceManager());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();

        if (message.equals("!exit")) {
            AudioManager audioManager = event.getGuild().getAudioManager();
            if (audioManager.isConnected()) {
                player.stopTrack();
                audioManager.closeAudioConnection();
                event.getChannel().sendMessage("Disconnected from voice channel.").queue();
            } else {
                event.getChannel().sendMessage("Not currently in a voice channel.").queue();
            }
            return;
        }

        if (message.startsWith("!play ")) {
            String trackUrl = message.substring("!play ".length()).trim();
            if (!trackUrl.startsWith("https://")) {
                trackUrl = "https://www.youtube.com/results?search_query=" + trackUrl;
            }

            AudioManager audioManager = event.getGuild().getAudioManager();
            VoiceChannel voiceChannel = event.getMember().getVoiceState().getChannel().asVoiceChannel();

            if (!audioManager.isConnected()) {
                if (voiceChannel == null) {
                    event.getChannel().sendMessage("You need to be in a voice channel to use this command!").queue();
                    return;
                }
                audioManager.openAudioConnection(voiceChannel);
                audioManager.setSendingHandler(sendHandler);
            }

            playerManager.loadItem(trackUrl, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    player.playTrack(track);

                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setTitle(track.getInfo().title);
                    builder.setDescription(track.getInfo().uri);
                    builder.setColor(Color.GREEN);

                    event.getChannel().sendMessageEmbeds(builder.build()).queue();
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    for (AudioTrack track : playlist.getTracks()) {
                        player.playTrack(track);
                    }
                }

                @Override
                public void noMatches() {
                    event.getChannel().sendMessage("No matches found.").queue();
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    event.getChannel().sendMessage("Load failed: " + exception.getMessage()).queue();
                }
            });
        }

        if (message.equals("!stop")) {
            player.stopTrack();
            event.getChannel().sendMessage("Stopped playback.").queue();
        }
    }
}