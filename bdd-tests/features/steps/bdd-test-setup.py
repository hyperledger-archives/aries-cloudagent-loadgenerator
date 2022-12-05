import json
import time
import os
import pprint
import random
import time

from behave import *
from starlette import status

from util import (
    call_issuer_verifier_service,
    call_holder_service,
    call_mediator_service,
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

MAX_INC = 10
SLEEP_INC = 2


@given('the issuer service is running')
def step_impl(context):
    # check that the issuer service is running
    issuer_status = call_issuer_verifier_service(context, "issuer", GET, "/status/config")
    assert "config" in issuer_status, pprint.pp(issuer_status)


@given('the holder service is running')
def step_impl(context):
    # check that the holder service is running
    holder_status = call_holder_service(context, "holder", GET, "/status/config")
    assert "config" in holder_status, pprint.pp(holder_status)


@given('the mediator service is running')
def step_impl(context):
    # check that the mediator service is running
    mediator_status = call_mediator_service(context, GET, "/status/config")
    assert "config" in mediator_status, pprint.pp(mediator_status)


@given('the issuer has "{config_name}" configured as "{config_value}"')
def step_impl(context, config_name: str, config_value: str):
    resp = set_issuer_config(context, "issuer", config_name, config_value)
    assert resp["config_value"] == config_value, pprint.pp(resp)


@given('there is a new holder agent "{holder_wallet}"')
def step_impl(context, holder_wallet: str):
    # create (and authenticate) a new holder agent
    clear_holder_context(context, holder_wallet)

    rand_suffix = ("000000" + str(random.randint(1,100000)))[-6:]
    holder_name = f"{holder_wallet}_{rand_suffix}"
    data = {
        "key_management_mode": "managed",
        "label": holder_name,
        "wallet_key": holder_name,
        "wallet_name": holder_name,
        "wallet_type": "indy",
    }

    holder_wallet_config = call_holder_service(context, "holder", POST, "/multitenancy/wallet", data=data)
    assert "token" in holder_wallet_config, pprint.pp(holder_wallet_config)

    put_holder_context(context, holder_wallet, "wallet", holder_wallet_config)

    # try calling the author wallet
    holder_config = call_holder_service(context, holder_wallet, GET, "/status/config")
    assert "config" in holder_config, pprint.pp(holder_config)


@given('the issuer connects with the mediator')
def step_impl(context):
    # get an invitation from the mediator
    resp = call_mediator_service(
        context,
        POST,
        "/connections/create-invitation",
        params={"auto_accept": "true"},
    )
    assert "invitation" in resp, pprint.pp(resp)
    invitation = resp["invitation"]

    # accept the invitation
    resp = call_issuer_verifier_service(
        context,
        "issuer",
        POST,
        "/connections/receive-invitation",
        data=invitation,
        params={"alias": "mediator", "auto_accept": "true"},
    )
    assert "connection_id" in resp, pprint.pp(resp)
    connection_id = resp["connection_id"]

    # wait for connection to go into "active" state
    mediator_conn = None
    inc = 0
    while not mediator_conn:
        resp = call_issuer_verifier_service(
            context,
            "issuer",
            GET,
            f"/connections/{connection_id}",
        )
        assert "connection_id" in resp, pprint.pp(resp)
        assert "state" in resp, pprint.pp(resp)
        if resp["state"] == "active":
            mediator_conn = resp
        else:
            inc += 1
            assert inc < MAX_INC, f"Error too many retries waiting for mediator connection"
            time.sleep(SLEEP_INC)

    # request mediationm on this connection
    data = {}
    resp = call_issuer_verifier_service(
        context,
        "issuer",
        POST,
        f"/mediation/request/{connection_id}",
        data=data,
    )
    assert "state" in resp, pprint.pp(resp)
    assert resp["state"] == "request", pprint.pp(resp)
    assert "mediation_id" in resp, pprint.pp(resp)
    mediation_id = resp["mediation_id"]

    mediator_req = None
    inc = 0
    while not mediator_req:
        resp = call_issuer_verifier_service(
            context,
            "issuer",
            GET,
            f"/mediation/requests/{mediation_id}",
        )
        assert "mediation_id" in resp, pprint.pp(resp)
        assert "state" in resp, pprint.pp(resp)
        if resp["state"] == "granted":
            mediator_req = resp
        else:
            inc += 1
            assert inc < MAX_INC, f"Error too many retries waiting for mediator request"
            time.sleep(SLEEP_INC)

    put_issuer_context(context, "issuer", "mediator_connection", mediator_conn)
    put_issuer_context(context, "issuer", "mediator_request", mediator_req)


@given('the holder connects with the mediator')
def step_impl(context):
    # get an invitation from the mediator
    resp = call_mediator_service(
        context,
        POST,
        "/connections/create-invitation",
        params={"auto_accept": "true"},
    )
    assert "invitation" in resp, pprint.pp(resp)
    invitation = resp["invitation"]
    print(invitation)

    # accept the invitation
    resp = call_holder_service(
        context,
        "holder",
        POST,
        "/connections/receive-invitation",
        data=invitation,
        params={"alias": "mediator", "auto_accept": "true"},
    )
    assert "connection_id" in resp, pprint.pp(resp)
    print(resp)
    connection_id = resp["connection_id"]

    # wait for connection to go into "active" state
    mediator_conn = None
    inc = 0
    while not mediator_conn:
        resp = call_holder_service(
            context,
            "holder",
            GET,
            f"/connections/{connection_id}",
        )
        assert "connection_id" in resp, pprint.pp(resp)
        assert "state" in resp, pprint.pp(resp)
        if resp["state"] == "active":
            mediator_conn = resp
        else:
            inc += 1
            assert inc < MAX_INC, f"Error too many retries waiting for mediator connection"
            time.sleep(SLEEP_INC)

    # request mediationm on this connection
    data = {}
    resp = call_holder_service(
        context,
        "holder",
        POST,
        f"/mediation/request/{connection_id}",
        data=data,
    )
    assert "state" in resp, pprint.pp(resp)
    assert resp["state"] == "request", pprint.pp(resp)
    assert "mediation_id" in resp, pprint.pp(resp)
    mediation_id = resp["mediation_id"]

    mediator_req = None
    inc = 0
    while not mediator_req:
        resp = call_holder_service(
            context,
            "holder",
            GET,
            f"/mediation/requests/{mediation_id}",
        )
        assert "mediation_id" in resp, pprint.pp(resp)
        assert "state" in resp, pprint.pp(resp)
        if resp["state"] == "granted":
            mediator_req = resp
        else:
            inc += 1
            assert inc < MAX_INC, f"Error too many retries waiting for mediator request"
            time.sleep(SLEEP_INC)

    put_holder_context(context, "holder", "mediator_connection", mediator_conn)
    put_holder_context(context, "holder", "mediator_request", mediator_req)


@when('the issuer generates an invitation "{with_or_without}" mediation')
def step_impl(context, with_or_without: str):
    data = None
    if with_or_without.lower() == "with":
        mediator_request = get_issuer_context(context, "issuer", "mediator_request")
        if mediator_request:
            data = {"mediation_id": mediator_request["mediation_id"]}
        else:
            assert False, "Error no mediator connection available for issuer"

    # get an invitation from the issuer
    resp = call_issuer_verifier_service(
        context,
        "issuer",
        POST,
        "/connections/create-invitation",
        data=data,
        params={"auto_accept": "true"},
    )
    assert "invitation" in resp, pprint.pp(resp)
    context.config.userdata["invitation"] = resp["invitation"]
    assert "connection_id" in resp, pprint.pp(resp)
    connection_id = resp["connection_id"]
    put_issuer_context(context, "issuer", "connection_id", connection_id)


@when('the holder accepts the invitation "{with_or_without}" mediation')
def step_impl(context, with_or_without: str):
    # accept the connection invitation
    invitation = context.config.userdata["invitation"]
    del context.config.userdata["invitation"]

    params = {"alias": "holder", "auto_accept": "true"}
    if with_or_without.lower() == "with":
        mediator_request = get_holder_context(context, "holder", "mediator_request")
        if mediator_request:
            params["mediation_id"] = mediator_request["mediation_id"]
        else:
            assert False, "Error no mediator connection available for holder"

    resp = call_holder_service(
        context,
        "holder",
        POST,
        "/connections/receive-invitation",
        data=invitation,
        params=params,
    )
    assert "connection_id" in resp, pprint.pp(resp)
    connection_id = resp["connection_id"]
    put_holder_context(context, "holder", "connection_id", connection_id)


@given('the issuer has an "{connection_status}" connection to the holder')
@then('the issuer has an "{connection_status}" connection to the holder')
def step_impl(context, connection_status: str):
    connection_id = get_issuer_context(context, "issuer", "connection_id")
    connection = None
    inc = 0
    while not connection:
        resp = call_issuer_verifier_service(
            context,
            "issuer",
            GET,
            f"/connections/{connection_id}"
        )
        assert "state" in resp, pprint.pp(resp)
        if resp["state"] == connection_status:
            connection = resp
        else:
            inc += 1
            assert inc < MAX_INC, "Error too many retries waiting for issuer connection"
            time.sleep(SLEEP_INC)


@given('the holder has an "{connection_status}" connection with the issuer')
@then('the holder has an "{connection_status}" connection with the issuer')
def step_impl(context, connection_status: str):
    connection_id = get_holder_context(context, "holder", "connection_id")
    connection = None
    inc = 0
    while not connection:
        resp = call_holder_service(
            context,
            "holder",
            GET,
            f"/connections/{connection_id}"
        )
        assert "state" in resp, pprint.pp(resp)
        if resp["state"] == connection_status:
            connection = resp
        else:
            inc += 1
            assert inc < MAX_INC, "Error too many retries waiting for holder connection"
            time.sleep(SLEEP_INC)


@then('the issuer has mediation active on the holder connection')
def step_impl(context):
    connection_id = get_issuer_context(context, "issuer", "connection_id")
    resp = call_issuer_verifier_service(
        context,
        "holder",
        GET,
        f"/connections/{connection_id}/endpoints",
    )
    assert "their_endpoint" in resp, pprint.pp(resp)
    assert "mediator" in resp["their_endpoint"], pprint.pp(resp)
