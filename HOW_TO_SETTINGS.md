## Loot Table
Each tier of loot crate/box has its own loot table file. These files are stored in a sub directory of the configuration folder. It is possible to change this directory in the main.cfg.

To add data to the tables all that is required is to add a new JSON entry.


### File format

File: .configs/wjlootboxes/loot/loot_table_tier_#.json -- # is the tier of loot box
--------------------------------------------------
{
    "loot_min_count": 1,    -- Lower limit of items to drop for the player, this is not item count by loot entries (lowest 1)
    "loot_min_count": 10,   -- Upper limit of items to drop for the player, will be randomly selected (keep value small to reduce lag)
    "loot_entries" :        -- Array of loo entries
    [
      {
        "item": "minecraft:stone",   -- Registry name of the item to drop, prefix with Ore@ to use ore dictionary
        "data": 0,                   -- Metadata value or damage of the item (subtypes, ignored for ore dictionary values)
        "min_count": 1,              -- Lower limit of stack size
        "max_count": 100,            -- Upper limit of stack size, if over max stack size will drop several items (keep small to reduce lag)
        "chance": 0.5                -- Chance to use this entry (Random Number < chance will result in a drop)
      },
      {
        -- each additional entry will be placed inside {} separated by ,
        -- any missing data will result in a crash, so do not skip entries
        -- NEI can be used to get id of blocks and items for loot table entries
      }
    ]
}
---------------------------------------------------


## Spawn data
Each world will have its own spawn settings. This allows customizing spawn rates, chances, or disabling loot box tiers per world.

Keep in mind when creating settings that all world share 1 thread for scanning and placing blocks. So do not set the delay between scans to high or the number of chunks to scan too low. As this will result in poor placement times.

### Placement mechanics
Placement is done at random in each chunk currently load in the world. When a chunk is selected it is checked for the number of placed boxes. If the number is less than max then the thread attempts to find a series or random positions. 

These positions are check against settings to see if they are valid. If a position is valid it is added to the placement queue. On the chance a position is not valid the thread will retry a limited number of times. At the end of this limit it will move to the next chunk assuming no valid positions can be found.

Additional to this when a random position is selected. The thread will attempt to move up and down a series of blocks. This is done to find an air block to place the box. 

After a thread has found placement position the main game thread takes over. In which at the end of the game tick boxes will be placed into the world. This is needed to prevent issues as a second thread can not place blocks inside the main thread.

### File format
File: .configs/wjlootboxes/spawning/spawn_settings_for_dim_#.json -- # is the dimension id, 0 is overload -1 nether 1 end
--------------------------------------------------
{
    "delay_between_scans": 36000000,        -- Time in mili-seconds (1000 per second) to wait before scanning the world again
    "delay_between_chunk_scans": 1000,      -- Time to delay after scanning several chunks, allows CPU to be used for other tasks
    "delay_to_rescan_chunk": 36000000,      -- Time to wait before scanning a chunk again
    "chunks_to_scan": 100,                  -- Number of chunks to scan before resting (delay_between_chunk_scans)
    "boxes_per_chunk": 1,                   -- Number of boxes to spawn in each chunk
    "tries_per_chunk": 3,                   -- Number of times to attempt to find a usable spot to place a box
    "height_adjust": 5,                     -- How far to move up and down to find a good placement spot
    "chances":                              -- Array of spawn chance weights
    [
        {
            "tier": 0,                      -- Tier of the box
            "chance": 0.3f                  -- Chance it will be selected for spawning
        },
        {
            "tier": 1,
            "chance": 0.2f
        },
        {
            "tier": 2,
            "chance": 0.1f
        },
        {
            "tier": 3,
            "chance": 0.05f
        },
        {
            "tier": 4,
            "chance": 0.01f
        }
    ],
    "blocks":                               -- Array of supported blocks to place a box
    [
        {
            "id": "minecraft:dirt",         -- id of the block (mod_id:block_id)
            "meta": 0                       -- optional, meta value of the block. Leave blank to use all 16 meta values.
        },
        {
            "id": "minecraft:dirt",
            "meta": 2                       
        }
        {
            "id": "minecraft:stone"        
        },
        {
            -- each additional entry will be placed inside {} separated by ,
            -- id is a required field, and must follow the format (mod_id:block_id)
            -- NEI can be used to get ids of blocks
        }
    ]
}
---------------------------------------------------