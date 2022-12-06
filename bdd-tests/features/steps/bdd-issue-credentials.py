import json
import time
import datetime
import os
import pprint
import random
import time
from requests import HTTPError

from behave import *
from starlette import status

from util import (
    call_issuer_verifier_service,
    call_holder_service,
    GET,
    POST,
    HEAD,
    get_issuer_context,
    put_issuer_context,
    clear_issuer_context,
    get_holder_context,
    put_holder_context,
    clear_holder_context,
    run_coroutine,
    issue_credential,
    receive_credential,
    issue_and_receive_credentials,
)


MAX_INC = 20
SLEEP_INC = 1
DISPLAY_INTERVAL = 100
LEDGER_URL = os.getenv("LEDGER_URL")
REVOC_REG_COUNT = 3000


@when("the issuer issues a credential to the holder")
def step_impl(context):
    # /issue-credential/send-offer
    connection_id = get_issuer_context(context, "issuer", "connection_id")
    cred_def = get_issuer_context(context, "issuer", "credential_definition")
    cred_def_id = cred_def["credential_definition_id"]

    (cred_id, resp) = run_coroutine(issue_credential, context, "issuer", connection_id, cred_def_id)

    # save into context
    assert "state" in resp, pprint.pp(resp)
    put_issuer_context(context, "issuer", "current_credential", resp)
    put_issuer_context(context, "issuer", "current_credential_id", cred_id)


@then("the holder holds a credential")
def step_impl(context):
    # /credentials
    cred_id = get_issuer_context(context, "issuer", "current_credential_id")
    (cred_id, credential) = run_coroutine(receive_credential, context, "holder", cred_id)

    # save into context
    assert credential["attrs"]["id_number"] == cred_id, pprint.pp(credential)
    put_holder_context(context, "holder", "current_credential", credential)
    put_holder_context(context, "holder", "current_credential_id", cred_id)


@when('the issuer issues "{total_cred_count}" credentials to the holder up to "{parallel_cred_count}" in parallel')
def step_impl(context, total_cred_count: str, parallel_cred_count: str):
    total_cred_count = int(total_cred_count)
    parallel_cred_count = int(parallel_cred_count)
    connection_id = get_issuer_context(context, "issuer", "connection_id")
    cred_def = get_issuer_context(context, "issuer", "credential_definition")
    cred_def_id = cred_def["credential_definition_id"]

    (success_count, failed_count, cred_ids_issued) = run_coroutine(
        issue_and_receive_credentials, context,
        "issuer", "holder",
        connection_id, cred_def_id,
        total_cred_count, parallel_cred_count,
    )
    assert len(cred_ids_issued) == total_cred_count, "Error success count only " + str(len(cred_ids_issued))

    put_issuer_context(context, "issuer", "current_credential_id_set", cred_ids_issued)


@then('the holder holds all "{total_cred_count}" credentials')
def step_impl(context, total_cred_count: str):
    total_cred_count = int(total_cred_count)
    cred_ids_issued = get_issuer_context(context, "issuer", "current_credential_id_set")
    issued_count = len(cred_ids_issued)
    assert issued_count == total_cred_count, f"Error only {issued_count} credentials issued"
    start_time = time.perf_counter()
    count = 0
    for cred_id in cred_ids_issued:
        (cred_id, credential) = run_coroutine(receive_credential, context, "holder", cred_id)
        assert credential["attrs"]["id_number"] == cred_id, pprint.pp(credential)
        count += 1
        if 0 == (count % DISPLAY_INTERVAL):
            print(f"Queried {count} credentials")

    processing_time = time.perf_counter() - start_time
    print(f"Completed {count} credentials in {processing_time}")
    creds_per_minute = count / (processing_time/60.0)
    print(f" -> {creds_per_minute} per minute")
    print("done")
