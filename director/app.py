import json
from typing import Dict, Union, Optional, Tuple
from googleapiclient import discovery
from flask import Flask
from mcstatus import MinecraftServer

app = Flask(__name__)


def poke_server() -> Tuple[Optional[str], bool]:
    compute = discovery.build("compute", "v1")
    instances = compute.instances()
    online = False
    ip = None

    with open("config.json") as config_file:
        config = json.load(config_file)

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


@app.route("/call")
def call() -> Dict[str, str]:
    ip, online = poke_server()
    status = "online" if online else "offline"
    return {"status": status, "url": ip}


if __name__ == "__main__":
    app.run()
