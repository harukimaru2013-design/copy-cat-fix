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

@Mixin(value = ICopycatBlockEntity.class, remap = false)
public interface ICopycatBlockEntityMixin extends ICopycatBlockEntity {

    @Overwrite
    static void read(ICopycatBlockEntity self, CompoundTag tag, boolean clientPacket) {
        if (tag.contains("EnableCT"))
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

        if (prevMaterial != self.getMaterial())
            BlockEntityUtils.redraw((BlockEntity) self);
    }
}

