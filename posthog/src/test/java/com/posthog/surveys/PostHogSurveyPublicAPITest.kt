package com.posthog.surveys

import com.posthog.internal.surveys.PostHogSurveyHelper
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class PostHogSurveyPublicAPITest {
    private fun createTestSurvey(
        id: String = "survey-123",
        name: String = "Test Survey",
        currentIteration: Int? = null,
        startDate: Date? = Date(),
        endDate: Date? = null,
    ): Survey {
        return Survey(
            id = id,
            name = name,
            type = SurveyType.POPOVER,
            questions = emptyList(),
            description = null,
            featureFlagKeys = null,
            linkedFlagKey = null,
            targetingFlagKey = null,
            internalTargetingFlagKey = null,
            conditions = null,
            appearance = null,
            currentIteration = currentIteration,
            currentIterationStartDate = null,
            startDate = startDate,
            endDate = endDate,
            schedule = null,
        )
    }

    // PostHogSurveyHelper tests

    @Test
    fun `getBaseSurveyEventProperties returns correct properties`() {
        val survey = createTestSurvey()
        val props = PostHogSurveyHelper.getBaseSurveyEventProperties(survey)

        assertEquals("survey-123", props["\$survey_id"])
        assertEquals("Test Survey", props["\$survey_name"])
    }

    @Test
    fun `getBaseSurveyEventProperties includes iteration when present`() {
        val survey = createTestSurvey(currentIteration = 3)
        val props = PostHogSurveyHelper.getBaseSurveyEventProperties(survey)

        assertEquals(3, props["\$survey_iteration"])
    }

    @Test
    fun `getBaseSurveyEventProperties excludes iteration when null`() {
        val survey = createTestSurvey(currentIteration = null)
        val props = PostHogSurveyHelper.getBaseSurveyEventProperties(survey)

        assertTrue(!props.containsKey("\$survey_iteration"))
    }

    @Test
    fun `getSurveyInteractionProperty returns correct key without iteration`() {
        val survey = createTestSurvey()
        val key = PostHogSurveyHelper.getSurveyInteractionProperty(survey, "responded")

        assertEquals("\$survey_responded/survey-123", key)
    }

    @Test
    fun `getSurveyInteractionProperty returns correct key with iteration`() {
        val survey = createTestSurvey(currentIteration = 2)
        val key = PostHogSurveyHelper.getSurveyInteractionProperty(survey, "responded")

        assertEquals("\$survey_responded/survey-123/2", key)
    }

    @Test
    fun `getSurveyInteractionProperty with zero iteration omits iteration`() {
        val survey = createTestSurvey(currentIteration = 0)
        val key = PostHogSurveyHelper.getSurveyInteractionProperty(survey, "dismissed")

        assertEquals("\$survey_dismissed/survey-123", key)
    }

    @Test
    fun `filterActiveMatchingSurveys returns active surveys`() {
        val activeSurvey = createTestSurvey(id = "active", startDate = Date(), endDate = null)
        val inactiveSurvey = createTestSurvey(id = "inactive", startDate = null, endDate = null)
        val endedSurvey = createTestSurvey(id = "ended", startDate = Date(), endDate = Date())

        val result =
            PostHogSurveyHelper.filterActiveMatchingSurveys(
                listOf(activeSurvey, inactiveSurvey, endedSurvey),
            ) { true }

        assertEquals(1, result.size)
        assertEquals("active", result[0].id)
    }

    @Test
    fun `filterActiveMatchingSurveys filters by feature flags`() {
        val survey =
            Survey(
                id = "flagged",
                name = "Flagged Survey",
                type = SurveyType.POPOVER,
                questions = emptyList(),
                description = null,
                featureFlagKeys = null,
                linkedFlagKey = "my-flag",
                targetingFlagKey = null,
                internalTargetingFlagKey = null,
                conditions = null,
                appearance = null,
                currentIteration = null,
                currentIterationStartDate = null,
                startDate = Date(),
                endDate = null,
                schedule = null,
            )

        val withFlagEnabled =
            PostHogSurveyHelper.filterActiveMatchingSurveys(listOf(survey)) { true }
        assertEquals(1, withFlagEnabled.size)

        val withFlagDisabled =
            PostHogSurveyHelper.filterActiveMatchingSurveys(listOf(survey)) { false }
        assertEquals(0, withFlagDisabled.size)
    }

    @Test
    fun `filterActiveMatchingSurveys returns empty for empty input`() {
        val result =
            PostHogSurveyHelper.filterActiveMatchingSurveys(emptyList()) { true }
        assertTrue(result.isEmpty())
    }
}
