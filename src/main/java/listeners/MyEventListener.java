package listeners;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;


import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class MyEventListener extends ListenerAdapter {

    static String API_KEY = Dotenv.load().get("API_KEY");
    static String tier="";
    static String winlose="";
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        String message = event.getMessage().getContentRaw();

        if (message.startsWith("!전적 ")) {
            String summonerName = message.substring("!전적 ".length());
            try {
                Summoner summoner = getSummonerByName(summonerName);
                String encryptedSummonerId = summoner.getId();
                LeagueEntry[] leagueEntries = getLeagueEntriesBySummonerId(encryptedSummonerId);
               printTier(leagueEntries);

                    String imageUrl="";
                for (LeagueEntry entry : leagueEntries) {
                    String imageUr = String.format("https://opgg-static.akamaized.net/images/medals/%s_%s.png", entry.getTier().toLowerCase(),romanToInt(entry.getRank()));
                    imageUrl=imageUr;
                }

                    EmbedBuilder builder = new EmbedBuilder();
                    builder.setColor(Color.GREEN)
                            .setTitle("소환사 프로필")
                            .setDescription("이것은 소환사의 프로필입니다.")
                            .setThumbnail(imageUrl) // imageUrl에는 티어 이미지 링크를 넣습니다.
                            .addField("티어", tier, false)
                            .addField("승패", winlose, false)
                            .addField("소환사명", summoner.getName(), false)
                            .addField("레벨", Integer.toString(summoner.getLevel()), false)
                            .addField("아이콘 ID", Integer.toString(summoner.getProfileIconId()), false);
                    channel.sendMessageEmbeds(builder.build()).queue();

//                String response = String.format("%s\n%s\n%s (Level %d, Icon ID %d)", tier,winlose,summoner.getName(), summoner.getLevel(), summoner.getProfileIconId());
//                channel.sendMessage(response).queue();
//                System.out.println(response);
            } catch (IOException e) {
                channel.sendMessage("An error occurred: " + e.getMessage()).queue();
                e.printStackTrace();
            }

        }

    }

    private static LeagueEntry[] getLeagueEntriesBySummonerId(String encryptedSummonerId) throws IOException {
        String requestUrl = "https://kr.api.riotgames.com/lol/league/v4/entries/by-summoner/" + encryptedSummonerId + "?api_key=" + API_KEY;
        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            Scanner sc = new Scanner(conn.getInputStream());
            String responseBody = sc.useDelimiter("\\Z").next();
            ObjectMapper objectMapper = new ObjectMapper();
            LeagueEntry[] leagueEntries = objectMapper.readValue(responseBody, LeagueEntry[].class);
            return leagueEntries;
        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }
    }

    private static void printTier(LeagueEntry[] leagueEntries) {
        if (leagueEntries == null || leagueEntries.length == 0) {
            System.out.println("Unranked");
        }

        for (LeagueEntry entry : leagueEntries) {
            if (entry.getQueueType().equals("RANKED_SOLO_5x5")) {
                tier=entry.getTier() + " " + entry.getRank()+ " "+entry.getLeaguePoints();
                winlose=entry.getWins()+"승 "+entry.getLosses()+"패 ";
            }
        }
    }
    public static int romanToInt(String s) {
        int result = 0;
        Map<Character, Integer> romanMap = new HashMap<Character, Integer>();
        romanMap.put('I', 1);
        romanMap.put('V', 5);

        for (int i = 0; i < s.length(); i++) {
            int currentValue = romanMap.get(s.charAt(i));
            int nextValue = 0;
            if (i < s.length() - 1) {
                nextValue = romanMap.get(s.charAt(i + 1));
            }
            if (currentValue < nextValue) {
                result -= currentValue;
            } else {
                result += currentValue;
            }
        }
        return result;
    }


    private Summoner getSummonerByName(String summonerName) throws IOException {
        String encodedName = encodeSummonerName(summonerName);
        System.out.println(encodedName);
        String requestUrl = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-name/" + encodedName + "?api_key=" + API_KEY;
        URL url = new URL(requestUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            Scanner sc = new Scanner(conn.getInputStream());
            String responseBody = sc.useDelimiter("\\Z").next();
            return parseSummoner(responseBody);
        } else {
            throw new IOException("HTTP error code: " + responseCode);
        }
    }

    private String encodeSummonerName(String summonerName) {
        return summonerName.replaceAll(" ", "%20");
    }

    private Summoner parseSummoner(String responseBody) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Summoner summoner = objectMapper.readValue(responseBody, Summoner.class);
        return summoner;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Summoner {
    @JsonProperty("accountId")
    private String accountId;
    @JsonProperty("profileIconId")
    private int profileIconId;
    @JsonProperty("revisionDate")
    private long revisionDate;
    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("puuid")
    private String puuid;
    @JsonProperty("summonerLevel")
    private int summonerLevel;

    public long getRevisionDate() {
        return revisionDate;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getPuuid() {
        return puuid;
    }

    public int getSummonerLevel() {
        return summonerLevel;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return summonerLevel;
    }

    public int getProfileIconId() {
        return profileIconId;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class LeagueEntry {
    @JsonProperty("queueType")
    private String queueType;
    @JsonProperty("tier")
    private String tier;
    @JsonProperty("rank")
    private String rank;
    @JsonProperty("leaguePoints")
    private int leaguePoints;
    @JsonProperty("wins")
    private int wins;
    @JsonProperty("losses")
    private int losses;

    public int getLeaguePoints() {
        return leaguePoints;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public String getQueueType() {
        return queueType;
    }

    public String getTier() {
        return tier;
    }

    public String getRank() {
        return rank;
    }
}