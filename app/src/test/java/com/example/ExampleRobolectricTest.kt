package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.BuiltInCatalog
import com.example.model.WidgetGridSize
import com.example.widget.WidgetBitmapRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `verify app_name and Android AppWidget bitmap rendering`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Widget Studio X", appName)

        val template = BuiltInCatalog.getTemplates().first()
        val telemetry = WidgetBitmapRenderer.readDeviceTelemetry(context)
        val bitmap = WidgetBitmapRenderer.renderWidgetToBitmap(
            gridSize = WidgetGridSize.SIZE_2X2,
            backgroundColorHex = template.backgroundColor,
            cornerRadius = template.cornerRadius,
            elements = template.elements,
            telemetry = telemetry
        )
        assertNotNull(bitmap)
        assertEquals(480, bitmap.width)
        assertEquals(480, bitmap.height)
        assertTrue(telemetry.batteryLevel in 1..100)
    }

    @Test
    fun `verify top-level Composable screen switcher navigates between My Projects, Visual Editor, and Settings`() {
        composeTestRule.waitForIdle()

        // Navigate to 'My Projects' via the top-level screen switcher
        composeTestRule.onNodeWithTag("switcher_my_projects").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("projects_screen").assertIsDisplayed()

        // Navigate to 'Visual Editor' via the top-level screen switcher
        composeTestRule.onNodeWithTag("switcher_visual_editor").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("visual_editor_screen").assertIsDisplayed()

        // Navigate to 'Settings' via the top-level screen switcher
        composeTestRule.onNodeWithTag("switcher_settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
    }
}
