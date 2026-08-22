package com.zleeper.sleepapp.domain.inventory

enum class EquipmentSlot { HEAD, CHARM, PACK, RELIC }
enum class ItemCategory { MATERIAL, EQUIPMENT, CONSUMABLE, QUEST, KEY, COSMETIC, COLLECTIBLE, RELIC }
enum class Rarity { COMMON, UNCOMMON, RARE, EPIC, MYTHIC }
data class ItemGrant(val itemId: String, val quantity: Int)
