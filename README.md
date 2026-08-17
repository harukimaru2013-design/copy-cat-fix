# Copycatfix

A small [Minecraft Forge](https://files.minecraftforge.net/) mixin patch mod for **Minecraft 1.20.1**
that fixes a bug in [Copycats+](https://modrinth.com/mod/copycats) (tested against `3.0.7+mc1.20.1`,
running with [Create](https://modrinth.com/mod/create) `6.0.7`/`6.0.8`).

## The bug

Copycat **Panel** and Copycat **Sliding Door** blocks that have a material inserted lose that
material (revert to an empty Copycat Base) when placed via a schematic — whether printed manually
(holding the schematic, right-click ghost, then "print" in creative) or fired from a
**Schematicannon**. Other Copycat types (Beam, Slab, Step, Ladder, ...) are unaffected.

### Root cause

In `ICopycatBlockEntity#read` (Copycats+), after the saved `Material` tag is parsed successfully,
the code re-validates it via:

```java
ICopycatBlock#getAcceptedBlockState(level, pos, consumedItem, null)
```

If this fails, the material *and* the consumed item are silently reset to empty
(`AllBlocks.COPYCAT_BASE`) — even though the `Material` tag itself was read correctly a few lines
earlier.

For schematic-placed **Panel** / **Sliding Door** blocks specifically, `consumedItem` (the "Item"
NBT tag recording what item was used to fill the copycat) ends up empty at read time.
`getAcceptedBlockState` immediately returns `null` for an empty item
(`ItemStack.EMPTY.getItem()` is not a `BlockItem`), so the revalidation always fails and the
legitimately-saved material gets wiped.

## The fix

This mod ships a single Mixin targeting `ICopycatBlockEntity#read` that removes the destructive
revalidation-reset step. The `Material` tag is trusted once read, exactly as it is for every other
Copycat type. No other behaviour is changed, and **Copycats+ itself is not modified or replaced** —
this is a standalone, additive patch mod.

See [`ICopycatBlockEntityMixin.java`](src/main/java/example/copycatfix/mixin/ICopycatBlockEntityMixin.java)
for the full implementation and reasoning.

## Requirements

- Minecraft **1.20.1**
- Minecraft Forge **47.4.21** (or compatible `[47,)`)
- [Create](https://modrinth.com/mod/create) **6.0.7+**
- [Copycats+](https://modrinth.com/mod/copycats) **3.0.7+**

## Installation

1. Download the latest `copycatfix-*.jar` from the [Releases](../../releases) page.
2. Drop it into your `mods` folder alongside Create and Copycats+ (**do not** replace or modify
   the Copycats+ jar — this mod works alongside it).
3. This is a **data-integrity fix that runs on world/schematic load**, so install it on **both the
   client and the server** (and on singleplayer, obviously) for consistent behaviour.

## Building from source

```
git clone https://github.com/<your-username>/copycatfix.git
cd copycatfix
./gradlew build
```

The built jar will be at `build/libs/copycatfix-<version>.jar`.

## License

[MIT](LICENSE)
