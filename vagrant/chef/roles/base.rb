name "base"
description "Install the base configurations that are needed for client or server"

run_list([
             # Default iptables setup on all servers.
             "recipe[apt]",
             "recipe[git]",
             "recipe[java]",
             "recipe[gradle::tarball]",
             "recipe[vim]",
             "recipe[ruby_build]",
             "recipe[rbenv::system]",
         ])

default_attributes({
       :rbenv => {
           :upgrade => true,
           :rubies => [
               {
                   :name => '2.0.0-p195',
                   :environment => {
                       'RUBY_CONFIGURE_OPTS' => '--enable-shared', # needs to be set for openstudio linking
                       'CONFIGURE_OPTS' => '--disable-install-doc'
                   }
               },
               {
                   :name => 'jruby-1.7.4',
               }
           ],
           :no_rdoc_ri => true,
           :global => "2.0.0-p195",
           :gems => {
               "2.0.0-p195" => [
                   {
                       :name => "rubygems-bundler",
                       :version => "1.2.2",
                   },
               ]
           }
       }
   }
)