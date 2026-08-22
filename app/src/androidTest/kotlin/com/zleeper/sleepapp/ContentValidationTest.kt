package com.zleeper.sleepapp

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zleeper.sleepapp.data.content.GameContentRepository
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentValidationTest {
    @Test fun completeContentPackHasNoBrokenReferencesOrMissingAssets() {
        val repository = GameContentRepository(ApplicationProvider.getApplicationContext())
        val errors = repository.validate()
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }
}
