import {createServer} from 'node:http'

createServer((req, res)=>{
    res.write(JSON.stringify([
                               {
                                 "relation" : [
                                   "delegate_permission/common.handle_all_urls",
                                   "delegate_permission/common.get_login_creds"
                                 ],
                                 "target" : {
                                   "namespace" : "android_app",
                                   "package_name" : "com.example.android",
                                   "sha256_cert_fingerprints" : [
                                     "C7:87:AC:46:8B:A2:84:2E:04:FD:C7:B5:EB:C5:35:BC:EA:9C:98:9D:3D:4D:D4:89:48:27:BC:10:BD:6E:3D:99"
                                   ]
                                 }
                               }
                             ]))
    res.end()
}).listen(8080)