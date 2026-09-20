package com.thorcompanion

object TrainerSprite {
    fun urlFor(trainer: Trainer): String? = trainer.spriteKey.takeIf { it.isNotBlank() }
        ?.let { "file:///android_asset/trainers/$it.png" }
}