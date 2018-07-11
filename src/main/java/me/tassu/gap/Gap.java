package me.tassu.gap;

import com.google.inject.Inject;
import lombok.Getter;
import lombok.val;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.HealEntityEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.text.format.TextColors;

import java.util.function.Function;

@Plugin(id = "gap", name = "Gap", description = "A really cool plugin!", version = "1.0.0")
public class Gap {

    private static final Function<Double, Double> ADD_ONE_TO_DOUBLE = in -> in + 1;

    @Getter private static Gap instance;
    @Getter private GapConfig config;

    public Gap() {
        instance = this;
    }

    @Inject @Getter private Logger logger;
    @Inject private Game game;

    @Inject @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configLoader;

    private int timer = 0;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("Ready to rock'n'roll!");

        config = new GapConfig(configLoader);
        config.save();

        val possiblePermissionsService = game.getServiceManager().provide(PermissionService.class);

        if (possiblePermissionsService.isPresent()) {
            val permissionService = possiblePermissionsService.get();

            val builder = permissionService.newDescriptionBuilder(this);

            builder.id("gap.bypass")
                    .description(Text.of("Allows the player to bypass the limiter."))
                    .assign(PermissionDescription.ROLE_STAFF, true)
                    .register();
        }

        val reloadCommand = CommandSpec.builder()
                .description(Text.of("Reloads the gap configuration."))
                .permission("gap.command.reload")
                .executor((src, args) -> {
                    config.load();

                    val message = Text.builder("[Gap] ").color(TextColors.DARK_GREEN)
                            .append(Text.builder("The configuration was reloaded.").color(TextColors.GREEN).build())
                            .build();

                    src.sendMessage(message);
                    return CommandResult.empty();
                })
                .build();

        Sponge.getCommandManager().register(this, reloadCommand, "gapreload", "reloadgap");

        Sponge.getScheduler().createTaskBuilder()
                .name("Gap healer task")
                .intervalTicks(10)
                .execute(() -> {
                    if (!config.isEnabled()) return;
                    if (!config.isEnableExperimentalHealing()) return;

                    timer++;

                    if (timer >= 5) {
                        timer = 0;
                    }

                    game.getServer().getOnlinePlayers().forEach(it -> {
                        val health = it.getHealthData().health();
                        val exhaustion = it.getFoodData().exhaustion();
                        val food = it.foodLevel().get();

                        if (health.get() < config.getMaxHpForRegen() && health.get() > 0) {
                            if (food >= 18 || (food >= 10 && timer == 0)) {
                                it.offer(health.transform(ADD_ONE_TO_DOUBLE));
                                it.offer(exhaustion.transform(ADD_ONE_TO_DOUBLE));
                                it.sendMessage(ChatTypes.ACTION_BAR, Text.of("§2HP++ [exh: " + exhaustion.get() + ", sat: " + it.saturation().get() +"]"));
                            }
                        }
                    });
                })
                .submit(this);
    }

    @Listener
    public void onGameStopping(GameStoppingEvent event) {
        logger.info("Bye!");
        config.save();
    }

    @Listener
    public void onHealEntity(HealEntityEvent event) {
        if (getConfig().isEnabled()) return; // whole thing disabled
        if (!(event.getTargetEntity() instanceof Player)) return; // not a player
        if (getConfig().isEnableExperimentalHealing()) {
            event.setCancelled(true);
            return; // use the scheduled thing
        }

        val player = (Player) event.getTargetEntity();

        if (getConfig().isEnableBypassPerm() && player.hasPermission("gap.bypass")) return; // has bypass perm
        if (player.getHealthData().health().get() < getConfig().getMaxHpForRegen()) return; // has fewer hp than required

        event.setCancelled(true);
    }
}
