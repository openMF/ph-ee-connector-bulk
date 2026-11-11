# payment-hub-ee
# Bulk System Connector
**Core Function**: Protocol-aware integration layer for bulk data exchange.

## Key Responsibilities
- Routes processed records to destination systems
- Transforms data formats (JSON↔XML↔CSV)
- Implements retry/backoff for failed deliveries
- Manages connection security and quotas

## Supported Destinations
| System        | Protocol | Adapter Class         |
|---------------|----------|-----------------------|
| Fineract      | REST     | `FineractAdapter`     |
| Mambu         | SOAP     | `MambuSoapClient`     |
| Tax Authority | SFTP     | `SftpGovernmentConnector` |

## Inputs
- Messages from `bulk.transactions.processed` queue
- Files in `/inbound` SFTP folder
- Direct API calls to `/api/connector/submit`

## Outputs
- Delivery receipts to callback URLs
- Failed transaction archives in S3
- System-specific API responses


