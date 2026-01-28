# Multi-Threaded Email Processing

## Overview

The multi-threaded email processing feature allows you to process emails from a mailbox using multiple parallel threads. This dramatically improves throughput when processing large numbers of emails.

## Key Features

✅ **Parallel Processing** - Process multiple emails simultaneously  
✅ **Thread-Safe** - Each thread has its own IMAP connection  
✅ **Configurable** - 1-10 threads based on your needs  
✅ **Flexible** - Works with any EmailProcessor implementation  
✅ **Observable** - Detailed per-message results and statistics  
✅ **Resilient** - Individual failures don't stop overall processing  

## Quick Start

### 1. Implement EmailProcessor

```java
package com.example;

import interfaces.comms.model.EmailMessage;
import interfaces.comms.service.EmailProcessor;
import java.util.*;

public class MyEmailProcessor implements EmailProcessor {
    @Override
    public Map<String, Object> processEmail(EmailMessage email) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // Your processing logic here
        System.out.println("Processing: " + email.getSubject());
        
        result.put("success", true);
        result.put("message", "Processed successfully");
        return result;
    }
}
```

### 2. Call the REST Endpoint

```bash
POST /api/imap/processMessages
{
  "mailboxHost": "imap.gmail.com",
  "mailboxUser": "user@gmail.com",
  "mailboxPassword": "app-password",
  "processorClassName": "com.example.MyEmailProcessor",
  "threadCount": 4,
  "maxMessages": 100
}
```

## How It Works

```
Main Thread
  ├─> Thread 1: Messages 1-25    (own connection)
  ├─> Thread 2: Messages 26-50   (own connection)
  ├─> Thread 3: Messages 51-75   (own connection)
  └─> Thread 4: Messages 76-100  (own connection)

Each thread processes its messages and reports results
```

## Parameters

| Parameter | Required | Default | Description |
|-----------|----------|---------|-------------|
| mailboxHost | Yes | - | IMAP server hostname |
| mailboxUser | Yes | - | Mailbox username |
| mailboxPassword | Yes | - | Mailbox password |
| processorClassName | Yes | - | Your EmailProcessor class |
| mailboxFolder | No | INBOX | Folder to process |
| threadCount | No | 4 | Number of threads (1-10) |
| maxMessages | No | 0 (all) | Max messages to process |
| processNewest | No | false | Process newest first |

## Example: Logging Processor

A simple logging processor is included for testing:

```bash
POST /api/imap/processMessages
{
  "mailboxHost": "imap.gmail.com",
  "mailboxUser": "user@gmail.com",
  "mailboxPassword": "app-password",
  "processorClassName": "interfaces.comms.examples.LoggingEmailProcessor",
  "threadCount": 2,
  "maxMessages": 10
}
```

## Performance Guide

| Mailbox Size | Recommended Threads | Notes |
|--------------|---------------------|-------|
| < 50 messages | 1-2 | Connection overhead not worth it |
| 50-500 messages | 2-4 | Good balance |
| 500+ messages | 4-6 | Process in batches |
| 5000+ messages | 6-10 | Multiple runs recommended |

## Best Practices

### ✅ DO

- Keep processors stateless
- Use thread-safe dependencies
- Handle exceptions gracefully
- Log appropriately
- Test with small batches first

### ❌ DON'T

- Use instance variables in processor
- Share mutable state between messages
- Hold large data in memory
- Process all messages at once if mailbox is huge

## Thread Safety Example

```java
// GOOD: Stateless processor
public class GoodProcessor implements EmailProcessor {
    @Override
    public Map<String, Object> processEmail(EmailMessage email) {
        // No instance variables - thread-safe
        return processAndReturn(email);
    }
}

// BAD: Stateful processor
public class BadProcessor implements EmailProcessor {
    private int count = 0; // DANGER: Not thread-safe!
    
    @Override
    public Map<String, Object> processEmail(EmailMessage email) {
        count++; // Race condition!
        return result;
    }
}
```

## Error Handling

Individual message failures don't stop processing:

```json
{
  "processedCount": 100,
  "successCount": 98,
  "errorCount": 2,
  "errors": [
    {
      "messageId": "<failed-msg@example.com>",
      "error": "Processing failed",
      "timestamp": 1234567890
    }
  ]
}
```

## Monitoring

Track processing via statistics:

```bash
GET /api/imap/stats
```

Returns counts of all email operations including multi-threaded processing.

## Summary

Multi-threaded email processing is perfect for:
- Batch processing large mailboxes
- Periodic email harvesting
- Email migration
- Bulk email analysis
- Any scenario with many emails to process

Start with 2-4 threads and adjust based on your needs!
