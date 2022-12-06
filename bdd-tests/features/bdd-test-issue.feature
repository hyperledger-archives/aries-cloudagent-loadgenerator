@TestIssue
Feature: Issuer Issue Credential

    @TestIssue-001
    Scenario: Issue a single credential from issuer to holder
        Given the issuer and holder are running
        And the issuer is ready to issue credentials "without" revocation support
        And the issuer and holder have a connection
        When the issuer issues a credential to the holder
        Then the holder holds a credential

    @TestIssue-002
    Scenario: Issue multiple credential from an issuer to the same holder
        Given the issuer and holder are running
        And the issuer is ready to issue credentials "without" revocation support
        And the issuer and holder have a connection
        When the issuer issues "1000" credentials to the holder up to "20" in parallel
        Then the holder holds all "1000" credentials
