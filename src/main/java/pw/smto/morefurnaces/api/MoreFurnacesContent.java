package pw.smto.morefurnaces.api;

import net.minecraft.resources.Identifier;

public interface MoreFurnacesContent {
    default Identifier getIdentifier() {
        throw new RuntimeException("Must override getIdentifier()");
    };
}
