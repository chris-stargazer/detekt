package io.gitlab.arturbosch.detekt.cli.console

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.ConsoleReport
import io.gitlab.arturbosch.detekt.api.Detektion
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.SingleAssign
import java.util.HashMap

/**
 * @author Artur Bosch
 */
class BuildFailureReport : ConsoleReport() {

	override val priority: Int = Int.MIN_VALUE

	private var buildConfig: Config by SingleAssign()
	private var weightsConfig: Config by SingleAssign()
	private var warning: Int by SingleAssign()
	private var fail: Int by SingleAssign()

	override fun init(config: Config) {
		buildConfig = config.subConfig("build")
		weightsConfig = buildConfig.subConfig("weights")
		warning = buildConfig.valueOrDefault("warningThreshold", -1)
		fail = buildConfig.valueOrDefault("failThreshold", -1)
	}

	override fun render(detektion: Detektion): String? {
		val smells = detektion.findings.flatMap { it.value }
		val ruleToRuleSetId = extractRuleToRuleSetIdMap(detektion)
		val amount = smells.map { it.weighted(ruleToRuleSetId) }.sum()

		if (fail.reached(amount)) {
			throw BuildFailure("Build failure threshold of $fail reached with $amount weighted smells!")
		} else if (warning.reached(amount)) {
			return "Warning: $amount weighted code smells found. " +
					"Warning threshold is $warning and fail threshold is $fail!"
		} else {
			return null
		}
	}

	private fun extractRuleToRuleSetIdMap(detektion: Detektion): HashMap<String, String> {
		return detektion.findings.mapValues { it.value.map { it.id }.toSet() }
				.map { map -> map.value.map { it to map.key }.toMap() }
				.fold(HashMap<String, String>()) { result, map -> result.putAll(map); result }
	}

	private fun Finding.weighted(ids: Map<String, String>): Int {
		val key = ids[id] // entry of ID > entry of RuleSet ID > default weight 1
		return weightsConfig.valueOrDefault(id,
				if (key != null) weightsConfig.valueOrDefault(key, 1) else 1)
	}
}

internal fun Int.reached(amount: Int): Boolean = !(this == 0 && amount == 0) && this != -1 && this <= amount

class BuildFailure(override val message: String?) : RuntimeException(message)