package com.thorcompanion

/**
 * GBA memory addresses for Pokemon Emerald (USA) v1.0, as used by the pret/pokeemerald
 * decompilation and read through RetroArch's READ_CORE_MEMORY (which addresses GBA memory
 * directly, the same way the already-verified map address 0x020322E4 is read).
 *
 * Values cross-verified against the Ironmon-Tracker Emerald address map and the
 * pret/pokeemerald source (symbol ordering / linker script). Isolated here so they can be
 * re-verified/tuned without touching the UI.
 */
object EmeraldAddresses {
    const val PLAYER_PARTY = 0x020244ECL          // struct Pokemon[6] (100 bytes each)
    const val PLAYER_PARTY_COUNT = 0x020244E9L    // u8
    const val ENEMY_PARTY = 0x02024744L           // struct Pokemon[6] (100 bytes each)
    const val ENEMY_PARTY_COUNT = 0x020244EAL     // u8
    const val BATTLE_TYPE_FLAGS = 0x02022FECL     // u32
    const val BATTLE_OUTCOME = 0x0202433AL        // u8
    const val BATTLE_MONS = 0x02024084L           // struct BattlePokemon[4] (88 bytes each)
    const val TRAINER_OPPONENT_A = 0x02038BCAL    // u16
    const val TRAINER_OPPONENT_B = 0x02038BCCL    // u16
    const val SAVE_BLOCK1_PTR = 0x03005D8CL       // gSaveBlock1Ptr (IWRAM): points to the ASLR-shifted save block

    // Location offset within struct SaveBlock1 (include/global.h): pos@0x00, location@0x04.
    const val SAVEBLOCK1_LOCATION = 0x04L

    // struct BattlePokemon layout (include/pokemon.h)
    const val BATTLE_POKEMON_SIZE = 0x58          // 88 bytes
    const val POKEMON_SIZE = 0x64                 // 100 bytes

    // Battle type flags (include/constants/battle.h)
    const val BATTLE_TYPE_DOUBLE = 0x1
    const val BATTLE_TYPE_LINK = 0x2
    const val BATTLE_TYPE_TRAINER = 0x8

    /**
     * Battler id of the enemy in a single battle. Battler ids encode (side, flank):
     * bit 0 = side (0 player, 1 opponent), bit 1 = flank (0 left, 1 right). So
     * battler 0 = player-left, 1 = opponent-left, 2 = player-right, 3 = opponent-right.
     */
    const val ENEMY_BATTLER = 1
}
