---


```
========================================================================================
SESSION 3: THE SALESFORCE AGENTFORCE DEEP-DIVE
Prompt Builder Grounding, Multi-Input Flex Templates, & Agentforce Topic Orchestration
========================================================================================
```

---

## 1. Executive Summary & Architectural Overview

Session 3 shifts focus from external application frameworks directly into the **native Salesforce GenAI and Agentforce runtime**. 

Rather than treating Agentforce as a conversational black box, this session dissects:
1. **Prompt Builder Grounding:** Compiling deterministic CRM data, Apex logic, Flows, and nested templates into a unified context payload.
2. **The Agentforce DSL (Domain-Specific Language):** The underlying YAML-based state engine powering the Agent Router, Subagents (Topics), Session Variables, and Guardrails.
3. **Multi-Action Topic Dispatch:** Empowering an autonomous subagent to resolve complex customer disputes by combining **Apex Actions**, **Flow Actions**, and **Prompt Template Actions**.

```
┌───────────────────────────────────────────────────────────────────────────┐
│                    AGENTFORCE AUTONOMOUS EXECUTION FLOW                   │
└───────────────────────────────────────────────────────────────────────────┘

 1. Inbound Dispute
    "Where is my order ORD-8821? I want a refund!"
         │
         ▼
 2. Agent Router (@model://sfdc_ai__DefaultEinsteinHyperClassifier)
    Evaluates: Intent = Dispute, State = @variables.isVerified == False
         │
         ▼
 3. Subagent: ServiceCustomerVerification
    Action: VerifyCustomerByEmail (Apex) -> Matches Contact "Sarah Connor"
    State Transition: Sets @variables.isVerified = True
         │
         ▼
 4. Subagent: OrderAndDisputeResolution
    ├── Tool 1 [Apex]: CheckLogisticsStatus (Discovers forklift damage)
    ├── Tool 2 [Prompt Template]: Case_Triage_Executive_Brief (Grounds CRM + Loyalty)
    └── Tool 3 [Flow]: IssueGoodwillVoucher (Credits $50.00 to account)
         │
         ▼
 5. Output: Empathetic, fully resolved response with voucher & replacement details
```

---

## 2. Core Concepts Covered

### Prompt Template Architectures & Grounding
* **Standard vs. Flex Templates:** Standard templates (`Field Generation`, `Record Summary`) have fixed input contracts. **Flex Templates** accept open, heterogeneous input schemas (SObjects, primitives, nested contexts).
* **The `@` Resource System:**
  * Direct SObject Relationship Traversal: `{!$Input:Case.Contact.Email}`
  * Programmatic Context via Apex: `{!$Apex:Class.Property}` annotated with `capabilityType='FlexTemplate'`
  * Declarative Context via Flow: `{!$Flow:FlowName.Variable}`
  * Nested Prompt Composition: Embedding reusable prompt modules via `{!$PromptTemplate:Name}` to enforce enterprise guardrails.

### Agentforce DSL Anatomy
* **`agent_router`:** The primary classifier categorizing conversational intent and evaluating gate conditions.
* **`variables`:** Mutable session registers maintaining context across subagent transitions (e.g., identity verification states).
* **`subagent` (Topics):** Autonomous execution units defined by targeted system prompts, reasoning boundaries, and scoped tool authorizations.

---

## 3. Implementation Steps & Code Artifacts

### Part A: Prompt Builder Resources

#### 1. Grounding Apex: `OrderLogisticsGroundingService.cls`
```java
public with sharing class OrderLogisticsGroundingService {

    public class Request {
        @InvocableVariable(required=true label='Case Record' description='The Case being evaluated')
        public Case caseRecord;
    }

    public class Response {
        @InvocableVariable(label='Logistics Grounding Text' description='Extracted shipping and ERP telemetry')
        public String logisticsData;
    }

    @InvocableMethod(
        capabilityType='FlexTemplate' 
        label='Get Order Logistics Grounding' 
        description='Fetches real-time warehouse and carrier tracking telemetry for an order referenced in a Case'
    )
    public static List<Response> getLogisticsData(List<Request> requests) {
        List<Response> responses = new List<Response>();

        for (Request req : requests) {
            Response res = new Response();
            String caseDesc = req.caseRecord != null ? req.caseRecord.Description : '';
            
            if (caseDesc != null && caseDesc.contains('ORD-8821')) {
                res.logisticsData = 
                    'CARRIER TELEMETRY [FedEx Hub 4 - Chicago]:\n' +
                    '- Tracking: TRK-99201928\n' +
                    '- Status: CRITICAL_DELAY\n' +
                    '- Reason: Damaged pallet during forklift transfer\n' +
                    '- Original Delivery ETA: 2 days ago\n' +
                    '- Revised ETA: 4 business days';
            } else {
                res.logisticsData = 'CARRIER TELEMETRY: No known logistics delay flagged for this order.';
            }

            responses.add(res);
        }

        return responses;
    }
}
```

#### 2. The Master Flex Prompt Template: `Case_Triage_Executive_Brief`
* **Inputs:** `Case` (SObject: Case), `Agent_Notes` (Free Text)
* **Canvas Template Body:**
```text
You are an AI Service Operations Lead in Salesforce Service Cloud.

Your task is to analyze this customer dispute case and produce an executive triage summary with clear action items.

ADDITIONAL AGENT INSTRUCTIONS:
{!$Input:Agent_Notes}

CASE DETAILS (Direct Object Grounding):
- Case Number: {!$Input:Case.CaseNumber}
- Subject: {!$Input:Case.Subject}
- Priority: {!$Input:Case.Priority}
- Full Description: {!$Input:Case.Description}
- Customer Contact: {!$Input:Case.Contact.Name} ({!$Input:Case.Contact.Email})

LOGISTICS TELEMETRY (Grounded via Callable Apex):
{!$Apex:OrderLogisticsGroundingService.logisticsData}

CUSTOMER LOYALTY PROFILE (Grounded via Callable Flow):
{!$Flow:Calculate_Customer_Loyalty_Context.loyaltyContext}

POLICY & COMPLIANCE (Grounded via Child Prompt Template):
{!$PromptTemplate:Company_Tone_and_Safety_Guidelines}

INSTRUCTIONS:
1. Provide an executive summary of the issue.
2. Cross-reference the customer complaint with the carrier telemetry. Confirm if the delay is verified.
3. Because the customer is PLATINUM, recommend an immediate resolution action (e.g. overnight replacement or credit voucher).
```

---

### Part B: Refactoring Agentforce Identity Verification

Instead of requiring multi-step 2FA email verification codes, verification was streamlined into a single-turn email check.

#### Verification Apex Action: `AgentCustomerVerificationService.cls`
```java
public with sharing class AgentCustomerVerificationService {

    public class VerificationInput {
        @InvocableVariable(required=true label='Email Address' description='The customer email provided in the chat')
        public String emailAddress;
    }

    public class VerificationOutput {
        @InvocableVariable(label='Is Verified' description='True if customer exists in Salesforce')
        public Boolean isVerified;

        @InvocableVariable(label='Customer ID' description='Salesforce Contact ID')
        public String customerId;

        @InvocableVariable(label='Customer Name' description='Full Name of customer')
        public String customerName;

        @InvocableVariable(label='Verification Message' description='Confirmation message')
        public String verificationMessage;
    }

    @InvocableMethod(
        label='Verify Customer By Email' 
        description='Looks up a Contact by email to verify customer identity'
    )
    public static List<VerificationOutput> verifyCustomer(List<VerificationInput> inputs) {
        List<VerificationOutput> results = new List<VerificationOutput>();

        for (VerificationInput input : inputs) {
            VerificationOutput out = new VerificationOutput();
            
            List<Contact> contacts = [
                SELECT Id, Name, Email 
                FROM Contact 
                WHERE Email = :input.emailAddress 
                LIMIT 1
            ];

            if (!contacts.isEmpty()) {
                Contact c = contacts[0];
                out.isVerified = true;
                out.customerId = c.Id;
                out.customerName = c.Name;
                out.verificationMessage = 'Thank you, ' + c.Name + '! Your identity has been verified.';
            } else {
                out.isVerified = false;
                out.customerId = null;
                out.customerName = null;
                out.verificationMessage = 'We could not find an account associated with ' + input.emailAddress + '.';
            }
            results.add(out);
        }
        return results;
    }
}
```

---

### Part C: Custom Subagent Definition (`OrderAndDisputeResolution`)

```yaml
subagent OrderAndDisputeResolution:
    label: "Order and Dispute Resolution"
    description: "Handles shipping delays, lost packages, order disputes, and goodwill credits for verified customers."
    reasoning:
        instructions: ->
            | You assist verified customers with delayed or disputed shipments.
            | Step 1: When a customer asks about a delayed order, use {!@actions.CheckLogisticsStatus} to retrieve tracking telemetry.
            | Step 2: If the status is CRITICAL_DELAY, run the {!@actions.GenerateCaseBrief} prompt template to generate an executive briefing for the customer.
            | Step 3: For high-tier customers complaining of severe delays, offer and invoke {!@actions.IssueGoodwillVoucher} to grant a $50 goodwill credit.
            | Maintain an empathetic, professional tone at all times.
        actions:
            CheckLogisticsStatus: @actions.CheckLogisticsStatus
                with orderId = ...
            IssueGoodwillVoucher: @actions.IssueGoodwillVoucher
                with contactId = @variables.VerifiedCustomerId
                with amount = 50.00
            GenerateCaseBrief: @actions.GenerateCaseBrief
                with Case = ...
```

---

## 4. End-to-End Test Execution Transcript

* **Test Record Context:** 
  * Contact: `Sarah Connor` (`sarah.connor@cyberdyne.com`, Tier: `PLATINUM`)
  * Case: `#00001024` (`Urgent: Where is order ORD-8821? Need for event or refund!`)

```text
[User]  : Where is my order ORD-8821? It is late and I want a refund!
[Router]: Intent matches OrderAndDisputeResolution -> BLOCKED (@variables.isVerified == False)
          Transitions to ServiceCustomerVerification.
[Agent] : "I would be glad to help you with your order. For your security, could you please provide your email address first?"

[User]  : sarah.connor@cyberdyne.com
[Agent] : Invokes VerifyCustomerByEmail(emailAddress='sarah.connor@cyberdyne.com')
          Result: isVerified=True, customerId='003bm00000...', customerName='Sarah Connor'
          Transitions to agent_router -> OrderAndDisputeResolution.
[Agent] : "Thank you, Sarah Connor! Your identity has been verified. Let me check the details for order ORD-8821."

[Router]: Invokes CheckLogisticsStatus(orderId='ORD-8821')
          Result: CRITICAL_DELAY (Forklift damage, Chicago Hub)
          Invokes GenerateCaseBrief(Case='500bm00000...')
          Invokes IssueGoodwillVoucher(contactId='003bm00000...', amount=50.00)
          Result: Voucher CREDIT-5921 issued.
[Agent] : "Sarah, I checked order ORD-8821. It looks like it experienced a critical delay at our 
          Chicago distribution hub due to damaged packaging during transfer. 
          
          As a valued PLATINUM customer, I sincerely apologize for the frustration. I have 
          credited a $50 goodwill voucher to your account and prioritized an emergency 
          replacement package for guaranteed delivery before your event."
```

---

## 5. Homework & Hands-On Exercise

### Objective
Create an escalation path inside the `OrderAndDisputeResolution` subagent that detects legal or executive threats and transfers the conversation to a specialized queue.

### Specifications
1. **Flow / Action Creation:**
   * Create an Autolaunched Flow named `Escalate_To_Executive_Queue`.
   * Input: `caseId` (Text), `reason` (Text).
   * Logic: Update Case Priority to `"Critical"`, set Status to `"Escalated"`, and return confirmation string: `"ESCALATED_TO_EXECUTIVE_DESK"`.
2. **Subagent Instructions Update:**
   * Add a rule to `OrderAndDisputeResolution` instructions:
     > *"If the customer mentions the words 'lawyer', 'attorney', 'legal action', or threatens litigation, immediately invoke {!@actions.EscalateToExecutiveQueue} and transition to the escalation subagent. Do not issue a credit voucher."*
3. **Verification in Testing Console:**
   * Provide email: `sarah.connor@cyberdyne.com`.
   * Test input: *"Order ORD-8821 is missing and I will be contacting my attorney to take legal action against your company!"*
   * Assert that the agent does **not** offer a $50 credit and immediately logs the escalation to the executive team.
