const AWS = require('aws-sdk');

const dynamo = new AWS.DynamoDB.DocumentClient({
    params:{
        TableName: "tournamentScores"
    }
});

/**
 * Demonstrates a simple HTTP endpoint using API Gateway. You have full
 * access to the request and response payload, including headers and
 * status code.
 *
 * To scan a DynamoDB table, make a GET request with the TableName as a
 * query string parameter. To put, update, or delete an item, make a POST,
 * PUT, or DELETE request respectively, passing in the payload to the
 * DynamoDB API as a JSON body.
 */
exports.handler = async (event, context) => {
    //console.log('Received event:', JSON.stringify(event, null, 2));

    let body;
    let statusCode = '200';
    const headers = {
        'Content-Type': 'application/json',
    };

    try {
        switch (event.httpMethod) {
            case 'DELETE':
                body = await dynamo.delete(JSON.parse(event.body)).promise();
                break;
            case 'GET':
                //this is currently broken, as nothing calls it and the underlying table has changed
                const indexName = event.queryStringParameters.playerName ? "tournament-game-player-playerID-index" : undefined;
                const pkAttributeName = event.queryStringParameters.playerName ? "playerName" : "tournamentID";
                const pkAttributeValue = event.queryStringParameters.playerName ? event.queryStringParameters.playerName : event.queryStringParameters.tournamentID;

                body = await dynamo.query( {
                    Limit: 100,
                    IndexName: indexName,
                    KeyConditionExpression: '#pk = :val',
                    ExpressionAttributeNames: { '#pk': pkAttributeName },
                    ExpressionAttributeValues: { ':val': pkAttributeValue },
                    ScanIndexForward: true,
                }).promise();
                break;
            case 'POST':
                body = await dynamo.put(JSON.parse(event.body)).promise();
                break;
            case 'PUT':
                body = await dynamo.put({ Item: JSON.parse(event.body)}).promise();
                break;
            default:
                throw new Error(`Unsupported method "${event.httpMethod}"`);
        }
    } catch (err) {
        statusCode = '500';
        body = err.message;
    } finally {
        body = JSON.stringify(body);
    }

    return {
        statusCode,
        body,
        headers,
    };
};
