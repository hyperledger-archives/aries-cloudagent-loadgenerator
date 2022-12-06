import json
import time
import os
import pprint
import random
import time
from requests import HTTPError

from behave import *
from starlette import status

from util import (
    MAX_INC,
    SLEEP_INC,
    DISPLAY_INTERVAL,
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
)


LEDGER_URL = os.getenv("LEDGER_URL")
REVOC_REG_COUNT = 3000


@given('the issuer publishes a schema')
@when('the issuer publishes a schema')
def step_impl(context):
    # POST /schemas
    schema_name = "test_schema"
    schema_version = str(random.randrange(100)) + "." + str(random.randrange(100)) + "." + str(random.randrange(100))
    schema_attrs = ["full_name", "birthdate", "birthdate_dateint", "id_number", "favourite_colour",]
    schema = {
        "schema_name": schema_name,
        "schema_version": schema_version,
        "attributes": schema_attrs,
    }
    resp = call_issuer_verifier_service(
        context,
        "issuer",
        POST,
        f"/schemas",
        data=schema,
    )
    # save into context
    assert "schema" in resp, pprint.pp(resp)
    put_issuer_context(context, "issuer", "current_schema", resp["schema"])


@given('the issuer has an active schema on the ledger')
@when('the issuer has an active schema on the ledger')
@then('the issuer has an active schema on the ledger')
def step_impl(context):
    schema = get_issuer_context(context, "issuer", "current_schema")
    schema_id = schema["id"]

    # GET /schemas/created
    schemas_created = call_issuer_verifier_service(
        context,
        "issuer",
        GET,
        f"/schemas/created",
    )
    assert schema_id in schemas_created["schema_ids"], pprint.pp(schemas_created)

    # GET /schemas/{schema_id}
    schema_created = call_issuer_verifier_service(
        context,
        "issuer",
        GET,
        f"/schemas/{schema_id}",
    )
    assert "schema" in schema_created, pprint.pp(schema_created)


@given('the issuer publishes a credential definition "{with_or_without}" revocation support')
@when('the issuer publishes a credential definition "{with_or_without}" revocation support')
def step_impl(context, with_or_without: str):
    schema = get_issuer_context(context, "issuer", "current_schema")
    schema_id = schema["id"]
    tag = "test_tag"
    cred_def = {
        "schema_id": schema_id,
        "tag": tag,
    }
    if with_or_without.lower() == "with":
        cred_def["support_revocation"] = True
        cred_def["revoc_reg_count"] = REVOC_REG_COUNT
    resp = call_issuer_verifier_service(
        context,
        "issuer",
        POST,
        f"/credential-definitions",
        data=cred_def,
    )
    assert "sent" in resp, pprint.pp(resp)
    # save into context
    put_issuer_context(context, "issuer", "credential_definition", resp["sent"])


@given('the issuer has an active credential definition on the ledger')
@when('the issuer has an active credential definition on the ledger')
@then('the issuer has an active credential definition on the ledger')
def step_impl(context):
    cred_def = get_issuer_context(context, "issuer", "credential_definition")
    cred_def_id = cred_def["credential_definition_id"]

    # GET /credential-definitions/created
    cred_defs_created = call_issuer_verifier_service(
        context,
        "issuer",
        GET,
        f"/credential-definitions/created",
    )
    assert cred_def_id in cred_defs_created["credential_definition_ids"], pprint.pp(cred_defs_created)

    # GET /credential-definitions/{cred_def_id}
    cred_def_created = call_issuer_verifier_service(
        context,
        "issuer",
        GET,
        f"/credential-definitions/{cred_def_id}",
    )
    assert "credential_definition" in cred_def_created, pprint.pp(cred_def_created)
    # save into context
    put_issuer_context(context, "issuer", "current_credential_definition", cred_def_created["credential_definition"])


@given('the issuer has an active revocation registry on the ledger')
@then('the issuer has an active revocation registry on the ledger')
def step_impl(context):
    cred_def = get_issuer_context(context, "issuer", "credential_definition")
    cred_def_id = cred_def["id"]
    active_rev_reg = None
    inc = 0
    while not active_rev_reg:
        # GET /revocation/active-registry/{cred_def_id}
        try:
            resp = call_issuer_verifier_service(
                context,
                "issuer",
                GET,
                f"/revocation/active-registry/{cred_def_id}",
            )
            if "result" in resp and resp["result"]:
                active_rev_reg = resp["result"]
        except HTTPError:
            pass

        if not active_rev_reg:
            inc += 1
            assert inc <= MAX_INC, pprint.pp("Error too many retries can't find " + str(cred_def_id))
            time.sleep(SLEEP_INC)
