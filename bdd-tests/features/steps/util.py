import json
import os
import pprint
import requests
import time

from behave import *
from starlette import status


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

MAX_INC = 10
SLEEP_INC = 2


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
    print(data)
    data = json.dumps(data) if (data is not None) else None
    print(method, url)
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
