package example.copycatfix.mixin;

import com.copycatsplus.copycats.foundation.copycat.ICopycatBlockEntity;
import com.copycatsplus.copycats.utility.BlockEntityUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Fixes Copycat Panel / Copycat Sliding Door materials being silently wiped back to an empty
 * Copycat Base whenever they are placed via a schematic (Schematicannon fire OR creative-mode
 * "print").
 * <p>
 * Root cause (see {@code ICopycatBlockEntity#read} in Copycats+ 3.0.7+mc1.20.1): after parsing the
 * saved "Material" tag, upstream re-validates it via
 * {@code ICopycatBlock#getAcceptedBlockState(level, pos, consumedItem, null)} and, if that
 * revalidation does not exactly confirm the material, resets both the material and the consumed
 * item to empty. This revalidation is intended to reject materials that arrived via untrusted
 * network packets, but it also runs for ordinary NBT loads (chunk load, schematic placement) via
 * the same {@code clientPacket == false} code path. For Copycat Panel and Copycat Sliding Door
 * specifically, the shape/collision based acceptance check in
 * {@code ICopycatBlock#getAcceptedBlockState} can spuriously fail at the exact moment a schematic
 * places the block (before full world/shape context is settled), destroying legitimately saved
 * data.
 * <p>
 * NOTE: the Mixin target ({@code ICopycatBlockEntity}) is an interface, so this mixin must also be
 * declared as an interface (Mixin enforces target-type/mixin-type matching). Interface mixins in
 * Mixin 0.8.5 only support {@code @Overwrite} (and @Shadow/@Accessor/@Invoker) - injectors such as
 * {@code @Inject} are not supported there, hence the use of @Overwrite here. {@code remap = false}
 * is set directly on @Mixin (in addition to the mixins.json-level "remap": false) because the
 * annotation processor otherwise still tries to resolve an SRG/obfuscation mapping for @Overwrite
 * targets, which fails since this target is a modded (already-deobfuscated) interface, not a
 * vanilla Minecraft class.
 * <p>
 * This mixin keeps the behaviour identical to upstream except it no longer performs the
 * destructive revalidation reset. The material saved in the schematic/NBT is trusted and kept
 * as-is.
 */
@Mixin(value = ICopycatBlockEntity.class, remap = false)
public interface ICopycatBlockEntityMixin extends ICopycatBlockEntity {

    /**
     * @author copycatfix
     * @reason Removes the destructive material-revalidation reset that wipes Copycat Panel /
     * Copycat Sliding Door contents when read from a schematic. See class javadoc for details.
     */
    @Overwrite
    static void read(ICopycatBlockEntity self, CompoundTag tag, boolean clientPacket) {
        if (tag.contains("EnableCT")) // need to check because copycats migrated from C:Connected don't have this tag
            self.setCTEnabled(tag.getBoolean("EnableCT"));
        else
            self.setCTEnabled(true);

        self.setConsumedItem(ItemStack.of(tag.getCompound("Item")));

        BlockState prevMaterial = self.getMaterial();
        if (!tag.contains("Material")) {
            self.setConsumedItem(ItemStack.EMPTY);
            return;
        }

        self.setMaterialInternal(NbtUtils.readBlockState(self.blockHolderGetter(), tag.getCompound("Material")));

        // Upstream's material revalidation (and the destructive reset that follows a failed
        // revalidation) has intentionally been removed here. It only matters for untrusted network
        // packets (already excluded above via `clientPacket`), and produces false negatives for
        // Panel / Sliding Door materials read from schematics, wiping legitimate data.

        if (prevMaterial != self.getMaterial())
            BlockEntityUtils.redraw((BlockEntity) self);
    }
}

