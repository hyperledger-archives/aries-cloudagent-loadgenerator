import json
import datetime
import os
import pprint
import random
import requests
import time
import asyncio
import nest_asyncio
import multiprocessing.pool as mpool

from behave import *
from starlette import status


nest_asyncio.apply()


ISSUER_VERIFIER_API_KEY = os.getenv("ACAPY_ISSUER_API_ADMIN_KEY")
ISSUER_VERIFIER_BASE_URL = os.getenv("ACAPY_ISSUER_BASE_URL", "http://localhost:10000")

HOLDER_API_KEY = os.getenv("ACAPY_HOLDER_API_ADMIN_KEY")
HOLDER_BASE_URL = os.getenv("ACAPY_HOLDER_BASE_URL", "http://localhost:10010")


MEDIATOR_API_KEY = os.getenv("ACAPY_MEDIATOR_API_ADMIN_KEY")
MEDIATOR_BASE_URL = os.getenv("ACAPY_MEDIATOR_BASE_URL", "http://localhost:10002")

GET = "GET"
POST = "POST"
PUT = "PUT"
DELETE = "DELETE"
HEAD = "HEAD"
OPTIONS = "OPTIONS"

MAX_INC = 20
SLEEP_INC = 1
DISPLAY_INTERVAL = 100


def issuer_verifier_headers(context, token=None) -> dict:
    headers = {
        "accept": "application/json",
        "Content-Type": "application/json",
    }
    if ISSUER_VERIFIER_API_KEY:
        headers["X-API-KEY"] = ISSUER_VERIFIER_API_KEY
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def holder_headers(context, token=None) -> dict:
    headers = {
        "accept": "application/json",
        "Content-Type": "application/json",
    }
    if HOLDER_API_KEY:
        headers["X-API-KEY"] = HOLDER_API_KEY
    if token:
        headers["Authorization"] = f"Bearer {token}"
    return headers


def mediator_headers(context) -> dict:
    headers = {
        "accept": "application/json",
        "Content-Type": "application/json",
    }
    if MEDIATOR_API_KEY:
        headers["X-API-KEY"] = MEDIATOR_API_KEY
    return headers


def call_issuer_verifier_service(context, issuer_wallet, method, url_path, agency=False, data=None, params=None, json_data=True):
    """Call an http service on the issuer agent/agency (create wallet etc.)."""
    url = ISSUER_VERIFIER_BASE_URL + url_path
    issuer_config_key = f"issuer_{issuer_wallet}_config"
    if issuer_config_key in context.config.userdata and "wallet_token" in context.config.userdata[issuer_config_key]:
        token = context.config.userdata[issuer_config_key]["wallet_token"]
    else:
        token = None
    headers = issuer_verifier_headers(context, token=token)
    return call_http_service(method, url, headers, data=data, params=params, json_data=json_data)


def call_holder_service(context, holder_wallet, method, url_path, agency=False, data=None, params=None, json_data=True):
    """Call an http service on an holder agent/agency (accept invitation, etc.)."""
    url = HOLDER_BASE_URL + url_path
    holder_config_key = f"holder_{holder_wallet}_config"
    if holder_config_key in context.config.userdata and "wallet_token" in context.config.userdata[holder_config_key]:
        token = context.config.userdata[holder_config_key]["wallet_token"]
    else:
        token = None
    headers = holder_headers(context, token=token)
    return call_http_service(method, url, headers, data=data, params=params, json_data=json_data)


def call_mediator_service(context, method, url_path, data=None, params=None, json_data=True):
    """Call an http service on the mediator agent (accept invitation, etc.)."""
    url = MEDIATOR_BASE_URL + url_path
    headers = mediator_headers(context)
    return call_http_service(method, url, headers, data=data, params=params, json_data=json_data)


def call_http_service(method, url, headers, data=None, params=None, json_data=True):
    method = method.upper()
    data = json.dumps(data) if (data is not None) else None
    # print(method, url)
    if method == POST:
        response = requests.post(
            url=url,
            data=data,
            headers=headers,
            params=params,
        )
    elif method == GET:
        response = requests.get(
            url=url,
            headers=headers,
            params=params,
        )
    elif method == PUT:
        response = requests.put(
            url=url,
            data=data,
            headers=headers,
            params=params,
        )
    elif method == DELETE:
        response = requests.delete(
            url=url,
            headers=headers,
            params=params,
        )
    elif method == HEAD:
        response = requests.head(
            url=url,
            headers=headers,
            params=params,
        )
    elif method == OPTIONS:
        response = requests.options(
            url=url,
            headers=headers,
            params=params,
        )
    else:
        assert False, pprint.pp("Incorrect method passed: " + method)
    response.raise_for_status()
    if json_data:
        return response.json()
    else:
        return response.text


# manage bdd context for issuer(s)
def get_issuer_context(context, issuer_wallet: str, context_str: str, w_type: str = "issuer"):
    if (f"{w_type}_{issuer_wallet}_config" in context.config.userdata and 
        context_str in context.config.userdata[f"{w_type}_{issuer_wallet}_config"]):
        return context.config.userdata[f"{w_type}_{issuer_wallet}_config"][context_str]
    return None


def put_issuer_context(context, issuer_wallet: str, context_str: str, context_val, w_type: str = "issuer"):
    if not f"{w_type}_{issuer_wallet}_config" in context.config.userdata:
        context.config.userdata[f"{w_type}_{issuer_wallet}_config"] = {}
    context.config.userdata[f"{w_type}_{issuer_wallet}_config"][context_str] = context_val


def clear_issuer_context(context, issuer_wallet: str, context_str: str = None, w_type: str = "issuer"):
    if f"{w_type}_{issuer_wallet}_config" in context.config.userdata:
        if not context_str:
            del context.config.userdata[f"{w_type}_{issuer_wallet}_config"]
        elif context_str in context.config.userdata[f"{w_type}_{issuer_wallet}_config"]:
            del context.config.userdata[f"{w_type}_{issuer_wallet}_config"][context_str]


# manage bdd context for holder(s)
def get_holder_context(context, holder_wallet: str, context_str: str):
    return get_issuer_context(context, holder_wallet, context_str, w_type="holder")


def put_holder_context(context, holder_wallet: str, context_str: str, context_val):
    put_issuer_context(context, holder_wallet, context_str, context_val, w_type="holder")


def clear_holder_context(context, holder_wallet: str, context_str: str = None):
    clear_issuer_context(context, holder_wallet, context_str=context_str, w_type="holder")


# coroutine utilities
def run_coroutine(coroutine, *args, **kwargs):
    loop = asyncio.get_event_loop()
    if not loop:
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
    try:
        return loop.run_until_complete(coroutine(*args, **kwargs))
    finally:
        pass
        # loop.close()


def async_sleep(delay):
    run_coroutine(asyncio.sleep, delay)


# utility functions for issuing credentials, requesting proofs, etc.
async def issue_credential(context, issuer: str, connection_id: str, cred_def_id: str) -> (str, dict):
    age = 24
    d = datetime.date.today()
    birth_date = datetime.date(d.year-age, random.randint(1, 12), random.randint(1, 28))
    birth_date_format = "%Y%m%d"
    cred_id = str(random.randint(100000, 999999))
    cred_offer = {
        "auto_issue": True,
        "auto_remove": True,
        "comment": "string",
        "connection_id": connection_id,
        "cred_def_id": cred_def_id,
        "credential_preview": {
        "@type": "issue-credential/1.0/credential-preview",
        "attributes": [
            {
            "name": "full_name",
            "value": "martini"
            },
            {
            "name": "birthdate",
            "value": str(birth_date)
            },
            {
            "name": "birthdate_dateint",
            "value": birth_date.strftime(birth_date_format)
            },
            {
            "name": "id_number",
            "value": cred_id
            },
            {
            "name": "favourite_colour",
            "value": "purple"
            },
        ]
        },
        "trace": False
    }
    resp = call_issuer_verifier_service(
        context,
        issuer,
        POST,
        "/issue-credential/send-offer",
        data=cred_offer,
    )
    # save into context
    assert "state" in resp, pprint.pp(resp)
    return (cred_id, resp)


async def receive_credential(context, holder: str, cred_id: str) -> (str, dict):
    wql = {"attr::id_number::value": cred_id}
    credential = None
    inc = 0
    while not credential:
        resp = call_holder_service(
            context,
            holder,
            GET,
            f"/credentials",
            params={"wql": json.dumps(wql)}
        )
        if "results" in resp and 0 < len(resp["results"]):
            credential = resp["results"][0]
            assert credential["attrs"]["id_number"] == cred_id, pprint.pp(credential)
        if not credential:
            inc += 1
            assert inc <= MAX_INC, pprint.pp("Error too many retries can't find credential " + cred_id)
            async_sleep(SLEEP_INC)

    return (cred_id, credential)


async def issue_and_receive_credentials(
    context,
    issuer: str, holder: str,
    connection_id: str, cred_def_id: str,
    total_cred_count: int, parallel_cred_count: int
) -> (int, int, dict):
    cred_ids_issued = []

    # set up a thread pool for issuing (issuer) and receiving (holder)
    pool = mpool.ThreadPool(parallel_cred_count)
    loop = asyncio.get_event_loop()
    tasks = []

    start_time = time.perf_counter()
    while len(cred_ids_issued) < total_cred_count:
        (cred_id, resp) = run_coroutine(issue_credential, context, "issuer", connection_id, cred_def_id)
        cred_ids_issued.append(cred_id)
        cred_task = loop.create_task(receive_credential(context, "holder", cred_id))
        tasks.append(cred_task)
        active_tasks = len([task for task in tasks if not task.done()])
        while active_tasks >= parallel_cred_count:
            # done is cumulative, includes the full set of "done" tasks
            done, pending = await asyncio.wait(tasks, return_when=asyncio.FIRST_COMPLETED)
            active_tasks = len(pending)

            # reset counters since we are counting *all* done tasks
            failed_count = 0
            success_count = 0
            for finished in done:
                done_result = finished.result()
                # TODO what to do with result
            active_tasks = len([task for task in tasks if not task.done()])

        if 0 == (len(cred_ids_issued) % DISPLAY_INTERVAL):
            count = len(cred_ids_issued)
            processing_time = time.perf_counter() - start_time
            print(f"Submitted {count} in {processing_time} ({active_tasks} active)")

    # wait for the current batch of credential posts to complete
    done, pending = await asyncio.wait(tasks, return_when=asyncio.ALL_COMPLETED)
    # reset counters since we are counting *all* done tasks
    failed_count = 0
    success_count = 0
    for finished in done:
        done_result = finished.result()
        # TODO what to do with result
    tasks = []

    count = len(cred_ids_issued)
    processing_time = time.perf_counter() - start_time
    print(f"Completed {count} in {processing_time}")
    creds_per_minute = count / (processing_time/60.0)
    print(f" -> {creds_per_minute} per minute")
    print("done")

    return (success_count, failed_count, cred_ids_issued)


def request_proof(context):
    pass


def provide_proof(context):
    pass


def validate_proof(context):
    pass


def revoke_credential(context):
    pass
