package me.tassu.gap;

import lombok.Getter;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;

import java.io.IOException;

public class GapConfig {

    private static final String HEADER = "This is the main configuration file of Gap.";

    private static final ConfigurationOptions LOADER_OPTIONS = ConfigurationOptions.defaults()
            .setHeader(HEADER);

    private ObjectMapper<GapConfig>.BoundInstance configMapper;
    private ConfigurationLoader<CommentedConfigurationNode> loader;

    GapConfig(ConfigurationLoader<CommentedConfigurationNode> loader) {
        this.loader = loader;

        try {
            this.configMapper = ObjectMapper.forObject(this);
        } catch (ObjectMappingException e) {
            throw new RuntimeException(e);
        }

        this.load();
    }

    void load() {
        try {
            this.configMapper.populate(this.loader.load(LOADER_OPTIONS));
        } catch (ObjectMappingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    void save() {
        try {
            SimpleCommentedConfigurationNode out = SimpleCommentedConfigurationNode.root();
            this.configMapper.serialize(out);
            this.loader.save(out);
        } catch (ObjectMappingException | IOException e) {
            e.printStackTrace();
        }
    }

    @Setting(comment = "Main switch. If false, nothing will be enabled.")
    @Getter
    private boolean enabled = true;

    @Setting(value = "max health for natural generation", comment = "Max HP (1 HP = 1/2 hearts) that can be regenerated to.")
    @Getter
    private int maxHpForRegen = 10;

    @Setting(value = "enable bypass permission", comment = "If true, usage of permission \"gap.bypass\" will be allowed.")
    @Getter
    private boolean enableBypassPerm = false;

    @Setting(value = "enable experimental healing", comment = "If true, all HealEntityEvents for Players will be " +
            "cancelled and a custom healing task will be used.\n HealEntityEvent is not implemented in latest " +
            "versions of Sponge.\n Not recommended if HealEntityEvent is implemented.")
    @Getter
    private boolean enableExperimentalHealing = true;
}
