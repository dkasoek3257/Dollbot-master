
import io.github.cdimascio.dotenv.Dotenv;
import listeners.MusicPlayerListener;
import listeners.MyEventListener;
import listeners.test;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import net.dv8tion.jda.api.entities.Activity;

import net.dv8tion.jda.api.requests.GatewayIntent;


public class Dollbot {
    static String token = Dotenv.load().get("TOKEN");
    public static void main(String[] args) {

        JDABuilder builder = JDABuilder.createDefault(token);
        builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.MESSAGE_CONTENT);
        builder.setActivity(Activity.playing("Test"));
        builder.addEventListeners(new test());
        builder.addEventListeners(new MyEventListener());
        builder.addEventListeners(new MusicPlayerListener());
        try {
            JDA jda = builder.build();
            jda.awaitReady();
            System.out.println("Bot is ready!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}