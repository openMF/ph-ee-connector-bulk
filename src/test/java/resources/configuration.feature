Feature: configuration test

  Scenario: Payment mode config test
    Given Application context is loaded
    When I assert the payment mode config
    Then I should get the non empty payment modes
    And I should be able fetch the mapping for mode "GSMA"
    And I should get enum value PAYMENT for mode "MOJALOOP"

  Scenario: Bulk connector bpmn name test
    Given Application context is loaded
    When I have payment mode "SLCB"
    And I have tenant as "gorilla"
    And I should be able fetch the mapping for mode "SLCB"
    Then I should get the bulk connector bpmn name "bulk_connector_slcb-gorilla"

  Scenario: External api payload config test
    Given Application context is loaded
    When I assert the external api payload config
    Then I should get the non empty external api payload config
    And I should be able fetch the payload setter for mode "GSMA"
