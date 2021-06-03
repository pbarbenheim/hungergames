package io.github.blitzbeule.hungergames.phases.pregame;

import io.github.blitzbeule.hungergames.Hungergames;
import io.github.blitzbeule.hungergames.storage.Match;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;


public class Fight implements Listener {

    private int matchNumber;
    private Match match;
    private Hungergames hg;
    private int field;
    private Player p1;
    private Player p2;
    private Location sp1;
    private Location sp2;
    private int deathHeight;
    private boolean rended;
    private int current;
    private boolean noMove;
    private long timestamp;
    private int timer;
    private boolean firstDamage;

    public Fight(int matchNumber, Match match, int field, Hungergames plugin) {
        this.matchNumber = matchNumber;
        this.match = match;
        hg = plugin;
        if (field < 1 || field > 2) {
            throw new IllegalArgumentException("field must be 1 or 2");
        } else {
            this.field = field;
        }
        p1 = match.getPlayers()[0];
        p2 = match.getPlayers()[1];
        sp1 = hg.getDsm().getConfig().getLocation("pregame.f-arena.spawns.field" + field + ".spawn1");
        sp2 = hg.getDsm().getConfig().getLocation("pregame.f-arena.spawns.field" + field + ".spawn2");
        deathHeight = (int) (Math.round(sp1.getY()) - 3);
        rended = false;
        current = 0;
        noMove = false;
        timestamp = -1;
        timer = 3;
        firstDamage = false;
    }

    void start() {
        hg.getServer().getPluginManager().registerEvents(this, hg);
        newRound();
    }

    void finish() {
        HandlerList.unregisterAll(this);
    }

    void newRound() {
        if (current % 2 == 0) {
            p1.teleport(sp1, PlayerTeleportEvent.TeleportCause.PLUGIN);
            p2.teleport(sp2, PlayerTeleportEvent.TeleportCause.PLUGIN);
        } else {
            p1.teleport(sp2, PlayerTeleportEvent.TeleportCause.PLUGIN);
            p2.teleport(sp1, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
        noMove = true;
        firstDamage = false;
        rended = false;
        timestamp = System.currentTimeMillis();
        timer = 3;
        (new BukkitRunnable() {
            @Override
            public void run() {
                if (timer > 0) {
                    p1.sendActionBar(Component.text(timer + " seconds"));
                    p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.HOSTILE, 1, 1);
                    p2.sendActionBar(Component.text(timer + " seconds"));
                    p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.HOSTILE, 1, 1);
                    timer -= 1;
                } else if (timer == 0) {
                    noMove = false;
                    p1.sendActionBar(Component.text("GO"));
                    p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.HOSTILE, 1, 1);
                    p2.sendActionBar(Component.text("GO"));
                    p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.HOSTILE, 1, 1);
                    cancel();
                }
            }
        }).runTaskTimer(hg, 20, 20);
    }

    void saveResults(Match.Result r1, Match.Result r2) {
        match.addResult(r1, r2);
        rended = true;
    }

    void endRound(Player loser) {
        if (!rended) {
            if (loser.equals(p1)) {
                saveResults(Match.Result.LOSS, Match.Result.WIN);
            } else if (loser.equals(p2)) {
                saveResults(Match.Result.LOSS, Match.Result.WIN);
            } else {
                throw new IllegalArgumentException("Player is not in fight");
            }
        }
        current++;
        if (current < match.getRounds()) {
            newRound();
        } else {
            finish();
        }
    }


    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!firstDamage) {
            long diff = System.currentTimeMillis() - timestamp;
            if (diff >= 11000) {
                saveResults(Match.Result.EVEN, Match.Result.EVEN);
            }
            firstDamage = true;
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.getPlayer().getScoreboardTags().contains("fight")) {
            return;
        }
        Player p;
        if (!(event.getPlayer().equals(p1) || event.getPlayer().equals(p2))) {
            return;
        }
        if (event.hasChangedPosition()) {
            if (noMove) {
                event.setCancelled(true);
                return;
            }
            if (event.getTo().getY() < deathHeight) {
                endRound(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer().getScoreboardTags().contains("fight")) {
            if (event.getPlayer().equals(p1)) {
                event.setRespawnLocation(sp1);
                endRound(p1);
            } else if (event.getPlayer().equals(p2)) {
                event.setRespawnLocation(sp2);
                endRound(p2);
            }
        }
    }


}