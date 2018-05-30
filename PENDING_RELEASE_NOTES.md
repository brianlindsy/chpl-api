
## Version 15.13.0
_Date TBD_

### New Features
* Added new service /activity/corrective_action_plans to return just legacy corrective action plan activities.
* Add new chart: New vs. Incumbent Developer chart
  * Update chart data generation application
  * Add API endpoint to retrieve chart data
  * Refactored other Chart data generation to increase speed
* Updated URLs and verbs for several REST endpoints
  * Old endpoints have been deprecated
* Add new chart: Count of Developers & Products by Edition & Status
  * Update chart data generation application
  * Add API endpoint to retrieve chart data

### Bug Fixes
* Handle SED boolean parsing and UCD Process existence mistmatch for 2014 upload.

---