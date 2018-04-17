//
//  SampleData.swift
//  SmART-FORM
//
//  Created by Siyang Zhang on 6/13/17.
//  Copyright © 2017 Siyang Zhang. All rights reserved.
//


import UIKit

class HealthSurveyController: UIViewController {
    
    @IBOutlet weak var scrollView: UIScrollView!
    
    @IBOutlet weak var dataSurvey: UIButton!
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(scrollView)
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
    }
    
    @IBAction func uploadData(_ sender: UIButton) {
        UIApplication.shared.openURL(NSURL(string: "https://osu.az1.qualtrics.com/jfe/form/SV_eIPLorec5u70O3z")! as URL)
    }
    
}