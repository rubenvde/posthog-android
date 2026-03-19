package com.posthog.internal.surveys

import com.posthog.PostHogInternal
import com.posthog.surveys.Survey

@PostHogInternal
public object PostHogSurveyHelper {
    public fun getBaseSurveyEventProperties(survey: Survey): Map<String, Any> {
        val props = mutableMapOf<String, Any>()

        props["\$survey_name"] = survey.name
        props["\$survey_id"] = survey.id

        survey.currentIteration?.let { iteration ->
            props["\$survey_iteration"] = iteration
        }

        survey.currentIterationStartDate?.let { startDate ->
            props["\$survey_iteration_start_date"] = startDate
        }

        return props
    }

    public fun getSurveyInteractionProperty(
        survey: Survey,
        property: String,
    ): String {
        val currentIteration = survey.currentIteration

        return if (currentIteration != null && currentIteration > 0) {
            "\$survey_$property/${survey.id}/$currentIteration"
        } else {
            "\$survey_$property/${survey.id}"
        }
    }

    public fun filterActiveMatchingSurveys(
        surveys: List<Survey>,
        isFeatureFlagEnabled: (String) -> Boolean,
    ): List<Survey> {
        return surveys.filter { survey ->
            // Filter out inactive surveys (must have start date and no end date)
            if (survey.startDate == null || survey.endDate != null) return@filter false

            // Check feature flags
            val allKeys = mutableListOf<String>()

            survey.linkedFlagKey?.takeIf { it.isNotEmpty() }?.let { allKeys.add(it) }
            survey.targetingFlagKey?.takeIf { it.isNotEmpty() }?.let { allKeys.add(it) }

            if (!canActivateRepeatedly(survey)) {
                survey.internalTargetingFlagKey?.takeIf { it.isNotEmpty() }?.let { allKeys.add(it) }
            }

            survey.featureFlagKeys?.forEach { keyVal ->
                val flagValue = keyVal.value
                if (keyVal.key.isNotEmpty() && !flagValue.isNullOrEmpty()) {
                    allKeys.add(flagValue)
                }
            }

            allKeys.all { isFeatureFlagEnabled(it) }
        }
    }
}
