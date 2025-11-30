Feature: Get Similar Products

  Scenario: Retrieve list of similar products
    Given there are the following products
      | id | name   | price  | availability |
      | 1  | Shirt  | 9.99   | true         |
      | 2  | Dress  | 19.99  | true         |
      | 3  | Blazer | 29.99  | false        |
      | 4  | Shoes  | 49.99  | true         |
      | 5  | Boots  | 149.99 | true         |
    And products the following similar products
      | id | similar |
      | 1  | 2,3  |
      | 2  | 1,3  |
      | 3  | 1,2 |
      | 4  | 5  |
      | 5  | 4  |
    When I search by product ID "1"
    Then the response status is "OK"
    Then the result contains
      | id | name   | price | availability |
      | 2  | Dress  | 19.99 | true         |
      | 3  | Blazer | 29.99 | false        |
    And the result has "2" products


  Scenario: Retrieve list of similar products fails if similar product details are not found
    Given there are the following products
      | id | name   | price  | availability |
      | 1  | Shirt  | 9.99   | true         |
      | 2  | Dress  | 19.99  | true         |
      | 3  | Blazer | 29.99  | false        |
      | 4  | Shoes  | 49.99  | true         |
    And products the following similar products
      | id | similar |
      | 1  | 2,3  |
      | 2  | 1,3  |
      | 3  | 1,2 |
      | 4  | 5  |
    And product ID "5" returns "Not Found"
    When I search by product ID "4"
    Then the response status is "INTERNAL_SERVER_ERROR"
    Then the result contains error "There has been an error fetching product by ID"


  Scenario: Should return error when an invalid param value is send
    Given there are the following products
      | id | name   | price  | availability |
      | 1  | Shirt  | 9.99   | true         |
      | 2  | Dress  | 19.99  | true         |
    And products the following similar products
      | id | similar |
      | 1  | 2  |
    When I search by product ID "INVALID_ID"
    Then the response status is "BAD_REQUEST"
    Then the result contains error "Invalid productId"
