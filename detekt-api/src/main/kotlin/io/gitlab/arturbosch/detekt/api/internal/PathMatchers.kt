package io.gitlab.arturbosch.detekt.api.internal

import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.PathMatcher

private val supportedSyntax = setOf("glob", "regex")

/**
 * Converts given [syntaxAndPattern] into a [PathMatcher] specified by [FileSystem.getPathMatcher].
 * If no syntax (glob or regex) is specified before the pattern, the glob syntax is assumed
 * if not otherwise specified by [defaultSyntax].
 */
fun pathMatcher(syntaxAndPattern: String, defaultSyntax: String = "glob"): PathMatcher {

	fun assumeDefaultSyntax(pattern: String) = "$defaultSyntax:$pattern"

	val syntax = syntaxAndPattern.substringBefore(":")
	val result = when (syntax) {
		syntaxAndPattern -> assumeDefaultSyntax(syntaxAndPattern)
		in supportedSyntax -> syntaxAndPattern
		else -> throw IllegalArgumentException("Unsupported syntax '$syntax' for a PathMatcher. See FileSystem.getPathMatcher.")
	}

	return FileSystems.getDefault().getPathMatcher(result)
}
