import json
from typing import Dict, Union, Optional, Tuple
from googleapiclient import discovery
from flask import Flask
from google.cloud import datastore
from mcstatus import MinecraftServer

compute = discovery.build("compute", "v1")
instances = compute.instances()
datastore_client = datastore.Client()

app = Flask(__name__)

with open("config.json") as config_file:
    global config
    # noinspection PyRedeclaration
    config = json.load(config_file)


def poke_server() -> Tuple[Optional[str], bool]:
    global config
    online = False
    ip = None

    instance = None
    # noinspection PyBroadException
    try:
        instance = instances.get(**config).execute()
    except:
        pass

    if not instance or instance["status"] != "RUNNING":
        # noinspection PyBroadException
        try:
            instances.start(**config).execute()
        except Exception as e:
            print(e)
            pass
        finally:
            return None, None
    elif instance["status"] == "RUNNING":
        ip = instance["networkInterfaces"][0]["accessConfigs"][0]["natIP"]
        try:
            MinecraftServer(ip).ping()
            online = True
        except Exception as e:
            print(e)
            pass

    return ip, online


@app.route("/poke")
def poke() -> Dict[str, str]:
    ip, online = poke_server()
    status = "online" if online else "offline"
    return {"status": status, "url": ip}


@app.route("/eligible/<uuid>")
def check_eligible(uuid) -> Dict[str, str]:
    kind = 'accounts'

    task_key = datastore_client.key(kind, uuid)

    eligible = False

    entity = datastore_client.get(task_key)

    exists = entity is not None

    if exists and "eligible" in entity:
        eligible = entity["eligible"]

    return {"eligible": eligible, "exists": exists}



if __name__ == "__main__":
    app.run()
