# Scorekeeper500
A simple app to help scorekeep the card game 500. This was originally a pet project that I worked on with my daughter as something fun for us to build together.

The scoring table can be found [here](https://images.squarespace-cdn.com/content/v1/5dc57300f66de96b7f66f3a9/1573240602271-HG3YRQH3DMF8QOVZ4VJ4/ke17ZwdGBToddI8pDm48kJZvfGz0N9EGvkxXmQaEA-BZw-zPPgdn4jUwVcJE1ZvWQUxwkmyExglNqGp0IvTJZUJFbgE-7XRK3dMEBRBhUpz1eZlxh6Rg-SNOaFc4i83GJRnbE_IpkrWl9uqTabfZozks53Xhz2vnUehUutVJ0QM/500+Score+Table+white.jpeg?format=750w)

There's lots of work to do to optimize the UI, but it functionally works.

Dropdown list indexes are used to perform lookups in a static score table.

Added a mechanism to upload game scores to DynamoDB via an API Gateway+Lambda interface. A possible future use could be for running a progressive tournament and tracking scores for all players. Currently there is no place that the data is read back from the table, but in the course of building the app, it was working at various points.

The js in the Lambda is the template that AWS provides tweaked to work with the table/schema I settled on.
