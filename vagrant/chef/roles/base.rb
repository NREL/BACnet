name "base"
description "Install the base configurations that are needed for client or server"

run_list([
             # Default iptables setup on all servers.
             "recipe[apt]",
             "recipe[git]",
             "recipe[java]",
             "recipe[gradle::tarball]"
         ])