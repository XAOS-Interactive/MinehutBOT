package com.minehut.discordbot.events;

import com.minehut.discordbot.Core;
import com.minehut.discordbot.commands.music.SkipCommand;
import com.minehut.discordbot.util.Bot;
import com.minehut.discordbot.util.Chat;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.events.guild.member.*;
import sx.blah.discord.handle.impl.events.shard.DisconnectedEvent;
import sx.blah.discord.handle.impl.events.shard.ReconnectSuccessEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.awt.*;
import java.util.Date;

/**
 * Created by MatrixTunnel on 11/28/2016.
 */
public class ServerEvents {

    private static String disconnectReason = "UNKNOWN";

    @EventSubscriber
    public void handle(ReadyEvent event) throws MissingPermissionsException {

        Core.discordConnection = true;
        Bot.updateUsers();

        Core.getMusicManager().getPlayerCreateHooks().register(player -> player.addEventListener(new AudioEventAdapter() {
            @Override
            public void onTrackStart(AudioPlayer player, AudioTrack track) {
                for (String id : Bot.getMusicTextChannels()) {
                    IChannel channel = Core.getDiscord().getChannelByID(id);
                    AudioPlayer song = Core.getMusicManager().getPlayer(channel.getGuild().getID()).getPlayer();

                    if (channel != null && song.getPlayingTrack() == track) {
                        IMessage msg = Chat.sendMessage("Now Playing: **" + track.getInfo().title + "**", channel);

                        SkipCommand.votes.clear();
                        VoiceEvents.playing.add(msg);
                    }
                }
            }

            @Override
            public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
                //Chat.removeMessage(msg);
                SkipCommand.votes.clear();
                if (VoiceEvents.playing == null) return;

                for (IMessage msg : VoiceEvents.playing) { //Has thrown a npe a few times. I think I fixed it with the break
                    String content = msg.getContent().toLowerCase();
                    if (content.contains(track.getInfo().title.toLowerCase())) {
                        Chat.removeMessage(msg);
                        VoiceEvents.playing.remove(msg);
                        break;
                    }
                }
            }
        }));

        Core.registerCommands();
        for (IGuild guild : Core.getDiscord().getGuilds()) {
            Core.getMusicManager().getPlayer(guild.getID()).setVolume(25);
        }

        for (String id : Bot.getMusicVoiceChannels()) {
            IVoiceChannel channel = Core.getDiscord().getVoiceChannelByID(id);
            if (channel.getConnectedUsers().size() >= 1 && !channel.getConnectedUsers().contains(Core.getDiscord().getOurUser())) {
                channel.join();
            }
        }

        Core.enabled = true;
        Core.log.info("Bot ready.");
    }

    @EventSubscriber
    public void handle(UserJoinEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
        if (event.getGuild() != Bot.getMainGuild()) return;
        IUser user = event.getUser();
        IGuild guild = event.getGuild();

        Bot.updateUsers();

        //TODO Test how old the account is when joining

        //if (event.getUser().getCreationDate().) {

        //}

        EmbedBuilder embed = Chat.getEmbed();

        if (user.getAvatarURL().contains("null.webp")) {
            embed.withThumbnail("https://discordapp.com/assets/dd4dbc0016779df1378e7812eabaa04d.png");
        } else {
            embed.withThumbnail(user.getAvatarURL());
        }

        //TODO Display name
        Chat.sendMessage(embed.withTitle(Chat.getFullName(user)).withDesc("*joined the server.* \n\n**Clickable:** <@!" + user.getID() + ">" +
                "\n**Account Creation:** " + user.getCreationDate()/*.format(DateTimeFormatter.RFC_1123_DATE_TIME)*/ + //TODO <-----
                "\n**Forums:** [`" + user.getName() + "`](https://www.minehut.com/" + user.getName().replace(" ", "") + ")")
                //.appendField("Account Creation:", user.getCreationDate().format(DateTimeFormatter.ISO_DATE_TIME), false)
                //.withFooterText("System time").withTimestamp(new Date().getTime())
                .withFooterText("System time").withTimestamp(new Date().getTime())
                .withColor(Chat.CUSTOM_GREEN), Bot.getLogChannel());

        //Chat.sendDiscordMessage(event.getUser().mention() + " _has joined the Discord server._");
        Core.log.info(event.getUser().getName() + " joined the Discord server.");

        //if (event.getGuild().getUsers().size() == 1000) {
        //    Chat.sendDiscordMessage(event.getUser().toString() + " is the 1000th Discord member! Be sure and give them a big warm welcome! Maybe even buy him Legend! :D",
        //            event.getGuild().getChannelByID("239599059415859200"));
        //}
    }

    @EventSubscriber
    public void handle(UserLeaveEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
        if (event.getGuild() != Bot.getMainGuild()) return;
        IUser user = event.getUser();

        EmbedBuilder embed = Chat.getEmbed();

        if (user.getAvatarURL().contains("null.webp")) {
            embed.withThumbnail("https://discordapp.com/assets/dd4dbc0016779df1378e7812eabaa04d.png");
        } else {
            embed.withThumbnail(user.getAvatarURL());
        }

        Chat.sendMessage(embed.withTitle(user.getName() + "#" + user.getDiscriminator()).withDesc("*left the server.* \n\n**Clickable:** <@!" + user.getID() + ">" +
                "\n**Forums:** [`" + user.getName() + "`](https://www.minehut.com/" + user.getName().replace(" ", "") + ")")
                .withFooterText("System time").withTimestamp(new Date().getTime())
                .withColor(Chat.CUSTOM_RED), Bot.getLogChannel());

        //Chat.sendDiscordMessage(event.getUser().mention() + " _has left the Discord server._");
        Core.log.info(event.getUser().getName() + " left the Discord server.");
        Bot.updateUsers();
    }

    @EventSubscriber
    public void handle(UserBanEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
        if (event.getGuild() != Bot.getMainGuild()) return;
        IUser user = event.getUser();
        IGuild guild = event.getGuild();

        if (user.getName().equals(user.getDisplayName(guild))) {
            Chat.sendMessage(Chat.getEmbed().withAuthorIcon(user.getAvatarURL()).withAuthorName(user.getName() + "#" + user.getDiscriminator())
                    .withAuthorUrl("https://minehut.com/" + user.getName())
                    .withDesc("was banned from the Discord server.").withColor(Color.RED), Bot.getLogChannel()); //TODO Different color
        } else {
            Chat.sendMessage(Chat.getEmbed().withAuthorIcon(user.getAvatarURL()).withAuthorName(user.getName() + "#" + user.getDiscriminator() + " (" + user.getDisplayName(guild) + ")")
                    .withAuthorUrl("https://minehut.com/" + user.getName())
                    .withDesc("was banned from the Discord server.").withColor(Color.RED), Bot.getLogChannel()); //TODO Different color
        }

        //Chat.sendDiscordMessage(event.getUser().mention() + " **was banned from Discord.**");
        Core.log.info(event.getUser().getName() + " was banned from Discord.");
        Bot.updateUsers();
    }

    @EventSubscriber
    public void handle(UserPardonEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
        if (event.getGuild() != Bot.getMainGuild()) return;
        IUser user = event.getUser();
        IGuild guild = event.getGuild();

        if (user.getName().equals(user.getDisplayName(guild))) {
            Chat.sendMessage(Chat.getEmbed().withAuthorIcon(user.getAvatarURL()).withAuthorName(user.getName() + "#" + user.getDiscriminator())
                    .withAuthorUrl("https://minehut.com/" + user.getName())
                    .withDesc("was unbanned from the Discord server.").withColor(Color.PINK), Bot.getLogChannel()); //TODO Different color
        } else {
            Chat.sendMessage(Chat.getEmbed().withAuthorIcon(user.getAvatarURL()).withAuthorName(user.getName() + "#" + user.getDiscriminator() + " (" + user.getDisplayName(guild) + ")")
                    .withAuthorUrl("https://minehut.com/" + user.getName())
                    .withDesc("was unbanned from the Discord server.").withColor(Color.PINK), Bot.getLogChannel()); //TODO Different color
        }

        //Chat.sendDiscordMessage(event.getUser().mention() + " **was unbanned from Discord.**");
        Core.log.info(user.getName() + " was unbanned from Discord.");
        Bot.updateUsers();
    }

    @EventSubscriber
    public void handle(NickNameChangeEvent event) throws RateLimitException, DiscordException, MissingPermissionsException {
        if (event.getGuild() != Bot.getMainGuild()) return;
        IUser user = event.getUser();

        if (!event.getNewNickname().orElseGet(user::getName).equals(user.getName())) {
            Chat.sendMessage(Chat.getEmbed().withDesc(user.mention() + " changed their name from `" +
                    event.getOldNickname().orElseGet(user::getName) + "` to `" + event.getNewNickname().orElseGet(user::getName) + "`")
                    .withColor(Color.ORANGE), Bot.getLogChannel());
        } else {
            Chat.sendMessage(Chat.getEmbed().withDesc(user.mention() + " reset their name to `" + event.getNewNickname().orElseGet(user::getName) + "`")
                    .withColor(Color.ORANGE), Bot.getLogChannel());
        }

        Core.log.info("\"" + event.getOldNickname().orElseGet(user::getName) + "\" is now known as \"" + event.getNewNickname().orElseGet(user::getName) + "\"");
    }

    @EventSubscriber
    public void handle(ReconnectSuccessEvent event) {
        Bot.updateUsers();
        //Core.broadcast("Connection to Discord has been reestablished! Disconnect reason: " + disconnectReason);
        Core.log.info("Connection to Discord has been reestablished!");
        Core.discordConnection = true;
    }

    @EventSubscriber
    public void handle(DisconnectedEvent event) throws DiscordException {
        if (event.getReason() != DisconnectedEvent.Reason.LOGGED_OUT) {
            if (event.getReason() != DisconnectedEvent.Reason.RECONNECT_OP) {
                disconnectReason = event.getReason().name();
            }

            Core.discordConnection = false;
            Core.log.error("Disconnected from Discord.");
        }
    }

}