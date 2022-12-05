@TestSetup
Feature: Author Endorser Connection

    @TestSetup-001
    Scenario: Setup issuer (with schema and cred def) and holder, and connect to mediator
        Given the issuer service is running
        And the holder service is running
        And the issuer publishes a schema
        And the issuer has an active schema on the ledger
        And the issuer publishes a credential definition "without" revocation support
        And the issuer has an active credential definition on the ledger
        And the holder connects with the mediator
        When the issuer generates an invitation "without" mediation
        And the holder accepts the invitation "with" mediation
        Then the issuer has an "active" connection to the holder
        And the holder has an "active" connection with the issuer
        And the issuer has mediation active on the holder connection

    @TestSetup-002
    Scenario: Setup issuer (with schema and cred def) and holder, and connect to mediator consolidated
        Given the issuer and holder are running
        And the issuer is ready to issue credentials "without" revocation support
        And the issuer and holder have a connection
        Then the issuer has an "active" connection to the holder
        And the holder has an "active" connection with the issuer
        And the issuer has mediation active on the holder connection
