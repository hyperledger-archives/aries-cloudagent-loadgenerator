package com.bka.ssi.generator.domain.objects

class ProofExchangeRecordDo(
    val id: String,
    val time: Long,
    val connectionId: String,
    val state: String,
    val isVerified: Boolean,
    val isValid: Boolean,
    val comment: ProofExchangeComment
) {
}

class ProofExchangeComment {
    val shouldBeValid: Boolean
    val revocationRegistryId: String?
    val revocationRegistryIndex: String?

    constructor(comment: String) {
        this.shouldBeValid = Regex("^${EXPECTED_TO_BE_VALID}.*").matches(comment)
        this.revocationRegistryId =
            Regex("${REVOCATION_REGISTRY_ID_PREFIX}: ([^ ]*) ").find(comment)?.groupValues?.get(1)
        this.revocationRegistryIndex =
            Regex("${REVOCATION_REGISTRY_INDEX_PREFIX}: ([\\d+])").find(comment)?.groupValues?.get(1)
    }

    constructor(shouldBeValid: Boolean, revocationRegistryId: String?, revocationRegistryIndex: String?) {
        this.shouldBeValid = shouldBeValid
        this.revocationRegistryId = revocationRegistryId
        this.revocationRegistryIndex = revocationRegistryIndex

    }

    companion object {
        private const val EXPECTED_TO_BE_VALID = "Expected to be valid"
        private const val EXPECTED_TO_BE_INVALID = "Expected to be invalid"
        private const val REVOCATION_REGISTRY_ID_PREFIX = "revocationRegistryId"
        private const val REVOCATION_REGISTRY_INDEX_PREFIX = "revocationRegistryIndex"

    }

    override fun toString(): String {
        var comment = ""

        if (shouldBeValid) {
            comment += EXPECTED_TO_BE_VALID
        } else {
            comment += EXPECTED_TO_BE_INVALID
        }

        comment += " (${REVOCATION_REGISTRY_ID_PREFIX}: ${revocationRegistryId} ${REVOCATION_REGISTRY_INDEX_PREFIX}: ${revocationRegistryIndex} )"

        return comment
    }
}
